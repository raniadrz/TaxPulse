import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Navbar } from './Navbar'
import { AiAssistant } from '@/components/ai/AiAssistant'
import { useAuth } from '@/hooks/useAuth'

/**
 * Authenticated application shell: sidebar + top bar + routed page content. Shared by staff and
 * client portal accounts; the menu is filtered by role and the office-wide copilot is staff-only.
 */
export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { hasRole } = useAuth()
  const isClient = hasRole('CLIENT')

  return (
    <div className="flex h-full">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
      {!isClient && <AiAssistant />}
    </div>
  )
}
