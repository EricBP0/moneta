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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Progress } from "@/components/ui/progress"
import { PiggyBank, Plus, Trash2, Bell, AlertTriangle, AlertCircle } from "lucide-react"

interface Category {
  id: number
  name: string
}

interface Budget {
  id: number
  monthRef: string
  categoryId: number | null
  limitCents: number
}

interface BudgetStatus {
  budgetId: number
  categoryId: number
  limitCents: number
  consumptionCents: number
  percent: number
  triggered80: boolean
  triggered100: boolean
}

const defaultForm = { monthRef: monthToday(), categoryId: "", limitCents: "" }

export default function BudgetsPage() {
  const { addToast } = useAppToast()
  const [month, setMonth] = useState(monthToday())
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [budgetStatus, setBudgetStatus] = useState<BudgetStatus[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState(defaultForm)
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [budgetsData, categoriesData, dashboardData] = await Promise.all([
        apiClient.get<Budget[]>(`/api/budgets?month=${month}`),
        apiClient.get<Category[]>("/api/categories"),
        apiClient.get<{ budgetStatus: BudgetStatus[] }>(`/api/dashboard/monthly?month=${month}`),
      ])
      setBudgets(budgetsData || [])
      setCategories(categoriesData || [])
      setBudgetStatus(dashboardData?.budgetStatus || [])
    } catch {
      addToast("Erro ao carregar orcamentos.", "error")
    } finally {
      setLoading(false)
    }
  }, [month, addToast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    try {
      const payload = {
        monthRef: form.monthRef,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        subcategoryId: null,
        limitCents: Number(form.limitCents),
      }
      await apiClient.post("/api/budgets", payload)
      addToast("Orcamento criado.", "success")
      setForm((prev) => ({ ...prev, limitCents: "" }))
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const deleteBudget = async (budgetId: number) => {
    if (!window.confirm("Deseja excluir este orcamento?")) return
    try {
      await apiClient.delete(`/api/budgets/${budgetId}`)
      addToast("Orcamento removido.", "success")
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const findStatus = (budgetId: number) => budgetStatus.find((s) => s.budgetId === budgetId)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Orcamentos</h1>
          <p className="text-muted-foreground">Controle limites e alertas.</p>
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

      {/* Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Plus className="h-5 w-5" />
            Novo orcamento
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2">
                <Label>Mes</Label>
                <Input
                  type="month"
                  value={form.monthRef}
                  onChange={(e) => setForm((prev) => ({ ...prev, monthRef: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Categoria</Label>
                <Select value={form.categoryId} onValueChange={(v) => setForm((prev) => ({ ...prev, categoryId: v }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Limite (centavos)</Label>
                <Input
                  type="number"
                  min="1"
                  value={form.limitCents}
                  onChange={(e) => setForm((prev) => ({ ...prev, limitCents: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Button type="submit">Salvar</Button>
              <Button variant="outline" asChild>
                <Link href="/alertas">
                  <Bell className="mr-2 h-4 w-4" />
                  Ver alertas
                </Link>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* List */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Lista de orcamentos</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-24 w-full" />
              ))}
            </div>
          ) : budgets.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhum orcamento cadastrado.</p>
          ) : (
            <div className="space-y-4">
              {budgets.map((budget) => {
                const status = findStatus(budget.id)
                const categoryName = categories.find((c) => c.id === budget.categoryId)?.name || "Sem categoria"
                return (
                  <div
                    key={budget.id}
                    className="p-4 rounded-lg bg-secondary/50 border border-border space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                          <PiggyBank className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <p className="font-medium text-foreground">{categoryName}</p>
                          <p className="text-sm text-muted-foreground">
                            Limite {formatCents(budget.limitCents)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {status?.triggered80 && !status?.triggered100 && (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-yellow-500/10 text-yellow-500">
                            <AlertTriangle className="h-3 w-3" />
                            80%
                          </span>
                        )}
                        {status?.triggered100 && (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-destructive/10 text-destructive">
                            <AlertCircle className="h-3 w-3" />
                            100%
                          </span>
                        )}
                        <Button variant="ghost" size="icon" onClick={() => deleteBudget(budget.id)}>
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </div>
                    {status && (
                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-sm">
                          <span className="text-muted-foreground">
                            {formatCents(status.consumptionCents)} / {formatCents(status.limitCents)}
                          </span>
                          <span className={`font-medium ${
                            status.triggered100 ? "text-destructive" :
                            status.triggered80 ? "text-yellow-500" : "text-primary"
                          }`}>
                            {formatPercent(status.percent)}
                          </span>
                        </div>
                        <Progress
                          value={Math.min(status.percent, 100)}
                          className={`h-2 ${
                            status.triggered100 ? "[&>div]:bg-destructive" :
                            status.triggered80 ? "[&>div]:bg-yellow-500" : ""
                          }`}
                        />
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
