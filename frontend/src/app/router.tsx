import { lazy, Suspense, type ReactNode } from 'react'
import { Route, Routes } from 'react-router-dom'
import { GuestRoute, ProtectedRoute } from '../features/auth/ProtectedRoute'
import { AppShell } from '../components/layout/AppShell'
import { FullScreenLoader, Loading } from '../components/ui/Feedback'

const IntroPage = lazy(() => import('../features/intro/IntroPage'))
const LoginPage = lazy(() => import('../features/auth/LoginPage'))
const RegisterPage = lazy(() => import('../features/auth/RegisterPage'))
const DashboardPage = lazy(() => import('../features/dashboard/DashboardPage'))
const ScenariosPage = lazy(() => import('../features/scenarios/ScenariosPage'))
const ScenarioBuilderPage = lazy(() => import('../features/scenarios/ScenarioBuilderPage'))
const SimulationPage = lazy(() => import('../features/simulation/SimulationPage'))
const ComparisonPage = lazy(() => import('../features/comparison/ComparisonPage'))
const HistoryPage = lazy(() => import('../features/history/HistoryPage'))
const SettingsPage = lazy(() => import('../features/settings/SettingsPage'))
const NotFoundPage = lazy(() => import('../features/intro/NotFoundPage'))

const page = (el: ReactNode) => <Suspense fallback={<Loading />}>{el}</Suspense>

export function AppRouter() {
  return (
    <Suspense fallback={<FullScreenLoader label="Loading" />}>
      <Routes>
        <Route path="/" element={<IntroPage />} />
        <Route element={<GuestRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={page(<DashboardPage />)} />
            <Route path="/scenarios" element={page(<ScenariosPage />)} />
            <Route path="/scenarios/new" element={page(<ScenarioBuilderPage />)} />
            <Route path="/scenarios/:id/edit" element={page(<ScenarioBuilderPage />)} />
            <Route path="/simulations/:id" element={page(<SimulationPage />)} />
            <Route path="/compare/:id" element={page(<ComparisonPage />)} />
            <Route path="/history" element={page(<HistoryPage />)} />
            <Route path="/settings" element={page(<SettingsPage />)} />
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
