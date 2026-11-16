package co.edu.udistrital.acciones.m3catalogomercados.domain;

import java.util.List;

public record Pagina<T>(List<T> content, int page, int size, long totalElements, long totalPages) {
    public Pagina {
        content = List.copyOf(content);
    }

    public static <T> Pagina<T> of(List<T> content, int page, int size, long total) {
        return new Pagina<>(content, page, size, total, (total + size - 1) / size);
    }

    public int getNumber() {
        return page;
    }

    public int getNumberOfElements() {
        return content.size();
    }

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return page >= Math.max(totalPages - 1, 0);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
