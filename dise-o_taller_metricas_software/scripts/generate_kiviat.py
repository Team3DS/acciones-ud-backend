import sys
import xml.etree.ElementTree as ET
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt


def radar_factory(num_vars):
    theta = np.linspace(0, 2 * np.pi, num_vars, endpoint=False)
    return theta


def normalize(value, max_val):
    return min(value / max_val, 1.0) if max_val > 0 else 0


def parse_metrics(xml_file):
    tree = ET.parse(xml_file)
    root = tree.getroot()
    metrics = {}
    sums = {"M0": 0.0, "M1": 0.0, "M5": 0.0}
    maxs = {"M10": 0.0}
    m13_values = []
    for m in root.findall(".//file/metrics/metric"):
        mid = m.get("id")
        try:
            value = float(m.text)
        except (ValueError, TypeError):
            continue
        if mid in sums:
            sums[mid] += value
        elif mid == "M10":
            if value > maxs["M10"]:
                maxs["M10"] = value
        elif mid == "M13":
            m13_values.append(value)
    total_methods = len(root.findall(".//method_metrics/method"))
    total_classes = sums["M5"]
    metrics["M0"] = sums["M0"]
    metrics["M1"] = sums["M1"]
    metrics["M6"] = total_methods / total_classes if total_classes > 0 else 0
    metrics["M7"] = sums["M1"] / total_methods if total_methods > 0 else 0
    metrics["M10"] = maxs["M10"]
    metrics["M13"] = sum(m13_values) / len(m13_values) if m13_values else 0
    return metrics


def generate_kiviat(metrics, output_file="kiviat.png"):
    selected = [
        ("M0", "Lineas de Codigo"),
        ("M1", "Sentencias"),
        ("M6", "Metodos/Clase"),
        ("M7", "Sentencias/Metodo"),
        ("M10", "Complejidad Max"),
        ("M13", "Profundidad Prom."),
    ]

    labels = [label for _, label in selected]
    values = [metrics.get(mid, 0) for mid, _ in selected]

    max_values = {
        "M0": max(values[0] * 1.5, 1000),
        "M1": max(values[1] * 1.5, 500),
        "M6": 15,
        "M7": 15,
        "M10": 25,
        "M13": 5,
    }

    normalized = []
    for i, (mid, _) in enumerate(selected):
        max_v = max_values.get(mid, values[i] * 1.5)
        normalized.append(normalize(values[i], max_v))

    N = len(selected)
    theta = radar_factory(N)

    fig, ax = plt.subplots(figsize=(8, 8), subplot_kw=dict(projection="polar"))
    fig.patch.set_facecolor("white")

    ax.set_rgrids([0.2, 0.4, 0.6, 0.8, 1.0], labels=["", "", "", "", ""], angle=90, fontsize=7)
    ax.set_ylim(0, 1)

    ax.plot(theta, normalized, "o-", linewidth=2, color="#7aa2f7", label="Proyecto actual")
    ax.fill(theta, normalized, alpha=0.25, color="#7aa2f7")

    ax.set_xticks(theta)
    ax.set_xticklabels(labels, fontsize=10)

    ax.set_title("GLUD Fried Chicken - Metricas SourceMonitor\nDiagrama Kiviat", weight="bold", size=13, y=1.1)

    for i, (label, val, norm) in enumerate(zip(labels, values, normalized)):
        angle = theta[i]
        ax.annotate(f"{val:.1f}", xy=(angle, norm), fontsize=8, ha="center", va="bottom", color="#333")

    plt.tight_layout()
    plt.savefig(output_file, dpi=150, bbox_inches="tight", facecolor="white")
    plt.close()
    print(f"Diagrama Kiviat generado: {output_file}")


if __name__ == "__main__":
    xml_file = sys.argv[1] if len(sys.argv) > 1 else "metrics.xml"
    output = sys.argv[2] if len(sys.argv) > 2 else "kiviat.png"
    metrics = parse_metrics(xml_file)
    generate_kiviat(metrics, output)
