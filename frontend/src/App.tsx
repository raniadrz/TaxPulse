import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import { ProtectedRoute } from '@/components/routing/ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import { Spinner } from '@/components/ui/Spinner'
import LoginPage from '@/pages/LoginPage'
import NotFoundPage from '@/pages/NotFoundPage'
import { STAFF_ROLES } from '@/types/api'

// Route-level code splitting keeps the initial bundle small.
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const ClientsPage = lazy(() => import('@/pages/ClientsPage'))
const ClientDetailPage = lazy(() => import('@/pages/ClientDetailPage'))
const ObligationsPage = lazy(() => import('@/pages/ObligationsPage'))
const DocumentsPage = lazy(() => import('@/pages/DocumentsPage'))
const UsersPage = lazy(() => import('@/pages/UsersPage'))
const PortalOverviewPage = lazy(() => import('@/pages/portal/PortalOverviewPage'))
const PortalObligationsPage = lazy(() => import('@/pages/portal/PortalObligationsPage'))
const PortalDocumentsPage = lazy(() => import('@/pages/portal/PortalDocumentsPage'))
const PortalAssistantPage = lazy(() => import('@/pages/portal/PortalAssistantPage'))
const PortalProfilePage = lazy(() => import('@/pages/portal/PortalProfilePage'))

export default function App() {
  return (
    <AuthProvider>
      <Suspense fallback={<Spinner className="h-full" />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route element={<ProtectedRoute roles={STAFF_ROLES} />}>
                <Route index element={<DashboardPage />} />
                <Route path="clients" element={<ClientsPage />} />
                <Route path="clients/:id" element={<ClientDetailPage />} />
                <Route path="obligations" element={<ObligationsPage />} />
                <Route path="documents" element={<DocumentsPage />} />
                <Route element={<ProtectedRoute roles={['ADMIN']} />}>
                  <Route path="users" element={<UsersPage />} />
                </Route>
              </Route>
              <Route path="portal" element={<ProtectedRoute roles={['CLIENT']} />}>
                <Route index element={<PortalOverviewPage />} />
                <Route path="obligations" element={<PortalObligationsPage />} />
                <Route path="documents" element={<PortalDocumentsPage />} />
                <Route path="assistant" element={<PortalAssistantPage />} />
                <Route path="profile" element={<PortalProfilePage />} />
              </Route>
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </AuthProvider>
  )
}
