"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { getAlertTypeLabel } from "@/lib/constants/labels"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Bell, CheckCircle, AlertTriangle, AlertCircle, CheckCheck } from "lucide-react"

interface Alert {
  id: number
  type: string
  message: string
  triggeredAt: string
  isRead: boolean
}

export default function AlertsPage() {
  const { addToast } = useAppToast()
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [loading, setLoading] = useState(true)

  const loadAlerts = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiClient.get<Alert[]>("/api/alerts")
      setAlerts(data || [])
    } catch {
      addToast("Erro ao carregar alertas.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadAlerts()
  }, [loadAlerts])

  const markAsRead = async (alertId: number) => {
    // Optimistic update
    const previousAlerts = [...alerts]
    setAlerts(prevAlerts => 
      prevAlerts.map(a => a.id === alertId ? { ...a, isRead: true } : a)
    )

    try {
      await apiClient.patch(`/api/alerts/${alertId}/read`, {})
      addToast("Alerta marcado como lido.", "success")
    } catch (err) {
      // Revert on error
      setAlerts(previousAlerts)
      const message = err instanceof Error ? err.message : "Erro ao marcar alerta"
      addToast(message, "error")
    }
  }

  const markAllAsRead = async () => {
    // Optimistic update
    const previousAlerts = [...alerts]
    setAlerts(prevAlerts => prevAlerts.map(a => ({ ...a, isRead: true })))

    try {
      await apiClient.patch("/api/alerts/read-all", {})
      addToast("Todos alertas marcados como lidos.", "success")
    } catch (err) {
      // Revert on error
      setAlerts(previousAlerts)
      const message = err instanceof Error ? err.message : "Erro ao marcar alertas"
      addToast(message, "error")
    }
  }

  const getIcon = (type: string) => {
    if (type.includes("100")) return <AlertCircle className="h-5 w-5 text-destructive" />
    if (type.includes("80")) return <AlertTriangle className="h-5 w-5 text-yellow-500" />
    return <Bell className="h-5 w-5 text-primary" />
  }

  const unreadCount = alerts.filter((a) => !a.isRead).length

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Alertas</h1>
          <p className="text-muted-foreground">
            {unreadCount > 0 ? `${unreadCount} nao lido(s).` : "Todos os alertas foram lidos."}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button variant="outline" onClick={markAllAsRead}>
            <CheckCheck className="mr-2 h-4 w-4" />
            Marcar todos como lidos
          </Button>
        )}
      </div>

      {/* List */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Historico</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 w-full" />
              ))}
            </div>
          ) : alerts.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <CheckCircle className="h-12 w-12 text-primary mb-4" />
              <p className="text-muted-foreground">Nenhum alerta disparado.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {alerts.map((alert) => (
                <div
                  key={alert.id}
                  className={`flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg border ${
                    alert.isRead
                      ? "bg-secondary/30 border-border opacity-60"
                      : "bg-secondary/50 border-border"
                  }`}
                >
                  <div className="flex items-start gap-4">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-secondary">
                      {getIcon(alert.type)}
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{getAlertTypeLabel(alert.type)}</p>
                      <p className="text-sm text-muted-foreground">{alert.message}</p>
                      <p className="text-xs text-muted-foreground mt-1">
                        {new Date(alert.triggeredAt).toLocaleString("pt-BR")}
                      </p>
                    </div>
                  </div>
                  {!alert.isRead && (
                    <Button variant="ghost" size="sm" onClick={() => markAsRead(alert.id)}>
                      <CheckCircle className="mr-2 h-4 w-4" />
                      Marcar como lido
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
