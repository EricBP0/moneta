"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { formatCents, formatPercent } from "@/lib/format"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Progress } from "@/components/ui/progress"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Target, Plus, Pencil, Trash2, DollarSign } from "lucide-react"

interface Goal {
  id: number
  name: string
  targetAmountCents: number
  currentAmountCents: number
  targetDate: string | null
  status: string
}

const defaultForm = {
  name: "",
  targetAmountCents: "",
  currentAmountCents: "",
  targetDate: "",
}

function formatMonthYear(dateString: string | null): string {
  if (!dateString || !dateString.includes("-")) return ""
  const parts = dateString.split("-")
  if (parts.length < 2) return ""
  const [year, month] = parts
  return `${month}/${year}`
}

export default function GoalsPage() {
  const { addToast } = useAppToast()
  const [goals, setGoals] = useState<Goal[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<Goal | null>(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [depositOpen, setDepositOpen] = useState(false)
  const [depositGoalId, setDepositGoalId] = useState<number | null>(null)
  const [depositAmount, setDepositAmount] = useState("")
  const [loading, setLoading] = useState(true)

  const loadGoals = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiClient.get<Goal[]>("/api/goals")
      setGoals(data || [])
    } catch {
      addToast("Erro ao carregar metas.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadGoals()
  }, [loadGoals])

  const openCreate = () => {
    setEditing(null)
    setForm(defaultForm)
    setIsFormOpen(true)
  }

  const openEdit = (goal: Goal) => {
    setEditing(goal)
    setForm({
      name: goal.name,
      targetAmountCents: String(goal.targetAmountCents),
      currentAmountCents: String(goal.currentAmountCents),
      targetDate: goal.targetDate ?? "",
    })
    setIsFormOpen(true)
  }

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!form.targetDate) {
      addToast("Informe a data alvo da meta.", "error")
      return
    }
    try {
      const payload = {
        name: form.name,
        targetAmountCents: Number(form.targetAmountCents),
        currentAmountCents: Number(form.currentAmountCents),
        targetDate: form.targetDate,
      }
      if (editing) {
        await apiClient.patch(`/api/goals/${editing.id}`, payload)
        addToast("Meta atualizada.", "success")
      } else {
        await apiClient.post("/api/goals", payload)
        addToast("Meta criada.", "success")
      }
      setIsFormOpen(false)
      loadGoals()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const deleteGoal = async (goalId: number) => {
    if (!window.confirm("Deseja desativar esta meta?")) return
    try {
      await apiClient.delete(`/api/goals/${goalId}`)
      addToast("Meta desativada.", "success")
      loadGoals()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const openDeposit = (goalId: number) => {
    setDepositGoalId(goalId)
    setDepositAmount("")
    setDepositOpen(true)
  }

  const submitDeposit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!depositGoalId) return
    try {
      await apiClient.post(`/api/goals/${depositGoalId}/deposit`, {
        amountCents: Number(depositAmount),
      })
      addToast("Deposito registrado.", "success")
      setDepositOpen(false)
      loadGoals()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const getPercent = (goal: Goal) => {
    if (goal.targetAmountCents === 0) return 0
    return Math.round((goal.currentAmountCents / goal.targetAmountCents) * 100)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Metas</h1>
          <p className="text-muted-foreground">Acompanhe seus objetivos financeiros.</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" />
          Nova meta
        </Button>
      </div>

      {/* List */}
      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      ) : goals.length === 0 ? (
        <Card className="bg-card border-border">
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <Target className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">Nenhuma meta cadastrada.</p>
            <Button className="mt-4" onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" />
              Criar primeira meta
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {goals.map((goal) => {
            const percent = getPercent(goal)
            return (
              <Card key={goal.id} className="bg-card border-border">
                <CardHeader className="flex flex-row items-start justify-between pb-2">
                  <div className="flex items-center gap-3">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                      <Target className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{goal.name}</p>
                      <span className={`text-xs font-medium ${
                        goal.status === "ACTIVE" ? "text-primary" : "text-muted-foreground"
                      }`}>
                        {goal.status}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(goal)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => deleteGoal(goal.id)}>
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">
                        {formatCents(goal.currentAmountCents)} / {formatCents(goal.targetAmountCents)}
                      </span>
                      <span className="font-medium text-primary">{formatPercent(percent)}</span>
                    </div>
                    <Progress value={Math.min(percent, 100)} className="h-2" />
                  </div>
                  {(() => {
                    const formattedDate = formatMonthYear(goal.targetDate)
                    return formattedDate ? (
                      <p className="text-xs text-muted-foreground">
                        Prazo: {formattedDate}
                      </p>
                    ) : null
                  })()}
                  <Button variant="outline" size="sm" className="w-full" onClick={() => openDeposit(goal.id)}>
                    <DollarSign className="mr-2 h-4 w-4" />
                    Depositar
                  </Button>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}

      {/* Goal Form Dialog */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Editar meta" : "Nova meta"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="space-y-2">
              <Label>Nome</Label>
              <Input
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                required
                className="bg-input border-border"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Valor objetivo (centavos)</Label>
                <Input
                  type="number"
                  min="1"
                  value={form.targetAmountCents}
                  onChange={(e) => setForm((prev) => ({ ...prev, targetAmountCents: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Valor atual (centavos)</Label>
                <Input
                  type="number"
                  min="0"
                  value={form.currentAmountCents}
                  onChange={(e) => setForm((prev) => ({ ...prev, currentAmountCents: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Data alvo</Label>
              <Input
                type="month"
                value={form.targetDate}
                onChange={(e) => setForm((prev) => ({ ...prev, targetDate: e.target.value }))}
                className="bg-input border-border"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>Cancelar</Button>
              <Button type="submit">Salvar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Deposit Dialog */}
      <Dialog open={depositOpen} onOpenChange={setDepositOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Depositar</DialogTitle>
          </DialogHeader>
          <form onSubmit={submitDeposit} className="space-y-4">
            <div className="space-y-2">
              <Label>Valor (centavos)</Label>
              <Input
                type="number"
                min="1"
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                required
                className="bg-input border-border"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setDepositOpen(false)}>Cancelar</Button>
              <Button type="submit">Depositar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
