import { useEffect, useRef, type ReactNode } from 'react'
import { Icon } from '@/components/Icon'

export default function PreviewDialog({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    const element = dialog.current
    element?.showModal()
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      element?.close()
      document.body.style.overflow = overflow
      if (previous?.isConnected) previous.focus()
    }
  }, [])
  return <dialog ref={dialog} className="preview-dialog" aria-labelledby="preview-dialog-title" onCancel={event => { event.preventDefault(); onClose() }} onClick={event => { if (event.target === event.currentTarget) onClose() }}>
    <div className="preview-dialog-content">
      <button className="modal-close icon-button" aria-label="Cerrar ventana" onClick={onClose}><Icon name="close"/></button>
      <span className="section-tag">SOLTECH · VISTA PREVIA</span>
      <h2 id="preview-dialog-title">{title}</h2>
      {children}
    </div>
  </dialog>
}
