import { useEffect, useState } from 'react'
import { Icon } from '@/components/Icon'
import { readAccount } from '@/store/account'
import Onboarding from '@/pages/Onboarding'
import Dashboard from '@/pages/Workspace'

export default function App() {
  const [account, setAccount] = useState(readAccount)
  const [notice, setNotice] = useState('')
  useEffect(() => {
    try { localStorage.setItem('soltech.account', JSON.stringify(account)) }
    catch { setNotice('El navegador no permite guardar el avance. Puedes continuar durante esta sesión.') }
  }, [account])
  useEffect(() => { if (!notice) return; const timer = setTimeout(() => setNotice(''), 5000); return () => clearTimeout(timer) }, [notice])
  return <>{account.step === 'dashboard' ? <Dashboard account={account} setAccount={setAccount} notify={setNotice}/> : <Onboarding account={account} setAccount={setAccount} notify={setNotice}/>}<div className="toast" role="status" aria-live="polite">{notice && <><Icon name="check"/>{notice}<button aria-label="Cerrar notificación" onClick={() => setNotice('')}><Icon name="close" size={16}/></button></>}</div></>
}
