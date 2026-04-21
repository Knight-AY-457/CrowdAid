import { useCallback, useEffect, useState } from 'react'
import { HeroSection } from '@/components/features/hero/HeroSection'
import { VolunteerDashboard } from '@/components/features/volunteer/VolunteerDashboard'
import { useLocation } from '@/hooks/useLocation'
import { emergencyService } from '@/services/api'
import type { Emergency } from '@/types'
import { toast } from 'sonner'

export default function VolunteerDashboardPage() {
  const { location, isLoading, error, refresh } = useLocation()
  const [activeSos, setActiveSos] = useState<Emergency | null>(null)
  const [updatingActiveSos, setUpdatingActiveSos] = useState(false)

  const loadActive = useCallback(async () => {
    try {
      const rows = await emergencyService.getMy(true)
      const current = rows.find((item) => item.status === 'PENDING' || item.status === 'IN_PROGRESS') ?? null
      setActiveSos(current)
    } catch {
      setActiveSos(null)
    }
  }, [])

  useEffect(() => {
    loadActive()
    const interval = setInterval(loadActive, 10000)
    return () => clearInterval(interval)
  }, [loadActive])

  const markHelpCompleted = async (sos: Emergency) => {
    setUpdatingActiveSos(true)
    try {
      await emergencyService.resolve(sos.id)
      toast.success('Help marked as completed.')
      await loadActive()
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? 'Could not mark this SOS as completed.')
    } finally {
      setUpdatingActiveSos(false)
    }
  }

  const markDontNeedHelp = async (sos: Emergency) => {
    setUpdatingActiveSos(true)
    try {
      await emergencyService.cancel(sos.id)
      toast.success('SOS cancelled. Help request cleared.')
      await loadActive()
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? 'Could not cancel this SOS.')
    } finally {
      setUpdatingActiveSos(false)
    }
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 py-10 px-4">
      <HeroSection
        userLocation={isLoading ? null : location}
        sosLocked={!!activeSos}
        activeSos={activeSos}
        sosLockMessage="You already have an active SOS. Use Help Completed or Don't Need Help Now."
        onMarkHelpCompleted={markHelpCompleted}
        onDontNeedHelp={markDontNeedHelp}
        isUpdatingLockedSos={updatingActiveSos}
        onSosSent={(created) => setActiveSos(created)}
      />
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Volunteer Dashboard</h1>
        <p className="text-sm text-gray-600 mb-6">
          As a volunteer, you can also raise SOS for yourself. Nearby emergencies are based on your browser location.
        </p>
        <VolunteerDashboard
          location={location}
          locationLoading={isLoading}
          locationError={error}
          refreshLocation={refresh}
        />
      </div>
    </main>
  )
}
