"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { apiClient } from "@/lib/api-client"
import { formatCents, formatPercent, monthToday } from "@/lib/format"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Progress } from "@/components/ui/progress"
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  PiggyBank,
  Bell,
  Target,
  ArrowRight,
  AlertCircle,
} from "lucide-react"

interface DashboardData {
  incomeCents: number
  expenseCents: number
  netCents: number
  byCategory: Array<{
    categoryId: number
    categoryName: string
    expenseCents: number
  }>
  budgetStatus: Array<{
    budgetId: number
    categoryId: number
    limitCents: number
    consumptionCents: number
    percent: number
    triggered80: boolean
    triggered100: boolean
  }>
  alerts: Array<{
    id: number
    type: string
    message: string
    triggeredAt: string
    isRead: boolean
  }>
  goalsSummary: Array<{
    goalId: number
    name: string
    percent: number
    status: string
    neededMonthlyCents: number
  }>
}

export default function DashboardPage() {
  const [month, setMonth] = useState(monthToday())
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const { addToast } = useAppToast()

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const response = await apiClient.get<DashboardData>(`/api/dashboard/monthly?month=${month}`)
      setData(response)
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro ao carregar dashboard"
      setError(message)
      addToast("Erro ao carregar dashboard.", "error")
    } finally {
      setLoading(false)
    }
  }, [month, addToast])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-muted-foreground">Resumo mensal e alertas.</p>
        </div>
        <div className="flex items-center gap-2">
          <Label htmlFor="month" className="text-muted-foreground">Mes</Label>
          <Input
            id="month"
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="w-40 bg-input border-border"
          />
        </div>
      </div>

      {loading && <DashboardSkeleton />}
      {error && (
        <Card className="bg-destructive/10 border-destructive/20">
          <CardContent className="flex items-center gap-3 py-4">
            <AlertCircle className="h-5 w-5 text-destructive" />
            <p className="text-destructive">{error}</p>
          </CardContent>
        </Card>
      )}

      {data && !loading && (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Receitas</p>
                    <p className="text-2xl font-bold text-primary">{formatCents(data.incomeCents)}</p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10">
                    <TrendingUp className="h-6 w-6 text-primary" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Despesas</p>
                    <p className="text-2xl font-bold text-destructive">{formatCents(data.expenseCents)}</p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-destructive/10">
                    <TrendingDown className="h-6 w-6 text-destructive" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Saldo</p>
                    <p className={`text-2xl font-bold ${data.netCents >= 0 ? 'text-primary' : 'text-destructive'}`}>
                      {formatCents(data.netCents)}
                    </p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-secondary">
                    <Wallet className="h-6 w-6 text-foreground" />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Categories */}
          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle className="text-lg">Gastos por categoria</CardTitle>
            </CardHeader>
            <CardContent>
              {data.byCategory.length === 0 ? (
                <p className="text-muted-foreground text-sm">Nenhum gasto categorizado.</p>
              ) : (
                <div className="space-y-3">
                  {data.byCategory.map((item) => (
                    <div key={item.categoryId} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                      <span className="text-foreground">{item.categoryName}</span>
                      <span className="font-medium text-foreground">{formatCents(item.expenseCents)}</span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Budget Status */}
          <Card className="bg-card border-border">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg flex items-center gap-2">
                <PiggyBank className="h-5 w-5 text-primary" />
                Status de orcamentos
              </CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link href="/orcamentos" className="flex items-center gap-1">
                  Ver todos <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {data.budgetStatus.length === 0 ? (
                <p className="text-muted-foreground text-sm">Nenhum orcamento configurado.</p>
              ) : (
                <div className="space-y-4">
                  {data.budgetStatus.map((budget) => (
                    <div key={budget.budgetId} className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-foreground">Categoria #{budget.categoryId || "—"}</span>
                        <div className="flex items-center gap-2">
                          <span className="text-sm text-muted-foreground">
                            {formatCents(budget.consumptionCents)} / {formatCents(budget.limitCents)}
                          </span>
                          <span className={`text-sm font-medium ${
                            budget.triggered100 ? 'text-destructive' : 
                            budget.triggered80 ? 'text-yellow-500' : 'text-primary'
                          }`}>
                            {formatPercent(budget.percent)}
                          </span>
                        </div>
                      </div>
                      <Progress 
                        value={Math.min(budget.percent, 100)} 
                        className={`h-2 ${
                          budget.triggered100 ? '[&>div]:bg-destructive' : 
                          budget.triggered80 ? '[&>div]:bg-yellow-500' : ''
                        }`}
                      />
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Alerts */}
          <Card className="bg-card border-border">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg flex items-center gap-2">
                <Bell className="h-5 w-5 text-primary" />
                Alertas do mes
              </CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link href="/alertas" className="flex items-center gap-1">
                  Ver todos <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {data.alerts.length === 0 ? (
                <p className="text-muted-foreground text-sm">Nenhum alerta disparado.</p>
              ) : (
                <div className="space-y-3">
                  {data.alerts.slice(0, 5).map((alert) => (
                    <div key={alert.id} className={`flex items-start justify-between py-2 border-b border-border last:border-0 ${alert.isRead ? 'opacity-50' : ''}`}>
                      <div>
                        <p className="font-medium text-foreground">{alert.type}</p>
                        <p className="text-sm text-muted-foreground">{alert.message}</p>
                      </div>
                      <span className="text-xs text-muted-foreground">
                        {new Date(alert.triggeredAt).toLocaleDateString('pt-BR')}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Goals */}
          <Card className="bg-card border-border">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg flex items-center gap-2">
                <Target className="h-5 w-5 text-primary" />
                Metas
              </CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link href="/metas" className="flex items-center gap-1">
                  Ver todas <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {data.goalsSummary.length === 0 ? (
                <p className="text-muted-foreground text-sm">Nenhuma meta ativa.</p>
              ) : (
                <div className="space-y-4">
                  {data.goalsSummary.map((goal) => (
                    <div key={goal.goalId} className="space-y-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-foreground">{goal.name}</p>
                          <p className="text-xs text-muted-foreground">{goal.status}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-medium text-primary">{formatPercent(goal.percent)}</p>
                          <p className="text-xs text-muted-foreground">
                            Necessario {formatCents(goal.neededMonthlyCents)}/mes
                          </p>
                        </div>
                      </div>
                      <Progress value={Math.min(goal.percent, 100)} className="h-2" />
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Card key={i} className="bg-card border-border">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div className="space-y-2">
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-8 w-24" />
                </div>
                <Skeleton className="h-12 w-12 rounded-lg" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
      <Card className="bg-card border-border">
        <CardHeader>
          <Skeleton className="h-6 w-48" />
        </CardHeader>
        <CardContent className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
