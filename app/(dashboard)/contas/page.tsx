"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { formatCents } from "@/lib/format"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Wallet, Pencil, Trash2, Plus } from "lucide-react"

interface Institution {
  id: number
  name: string
}

interface Account {
  id: number
  name: string
  type: string
  currency: string
  initialBalanceCents: number
  balanceCents: number
  institutionId: number | null
}

const defaultForm = {
  name: "",
  type: "CHECKING",
  currency: "BRL",
  initialBalanceCents: "",
  institutionId: "",
}

export default function AccountsPage() {
  const { addToast } = useAppToast()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<Account | null>(null)
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [accountsData, institutionsData] = await Promise.all([
        apiClient.get<Account[]>("/api/accounts"),
        apiClient.get<Institution[]>("/api/institutions"),
      ])
      setAccounts(accountsData)
      setInstitutions(institutionsData)
    } catch (err) {
      addToast("Erro ao carregar contas.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    try {
      const payload = {
        name: form.name,
        type: form.type,
        currency: form.currency,
        initialBalanceCents: Number(form.initialBalanceCents),
        institutionId: form.institutionId ? Number(form.institutionId) : null,
      }
      if (editing) {
        await apiClient.patch(`/api/accounts/${editing.id}`, payload)
        addToast("Conta atualizada.", "success")
      } else {
        await apiClient.post("/api/accounts", payload)
        addToast("Conta criada.", "success")
      }
      setForm(defaultForm)
      setEditing(null)
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const editAccount = (account: Account) => {
    setEditing(account)
    setForm({
      name: account.name,
      type: account.type,
      currency: account.currency,
      initialBalanceCents: String(account.initialBalanceCents),
      institutionId: account.institutionId ? String(account.institutionId) : "",
    })
  }

  const deleteAccount = async (accountId: number) => {
    if (!window.confirm("Deseja desativar esta conta?")) return
    try {
      await apiClient.delete(`/api/accounts/${accountId}`)
      addToast("Conta desativada.", "success")
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const cancelEdit = () => {
    setEditing(null)
    setForm(defaultForm)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Contas</h1>
        <p className="text-muted-foreground">Gerencie contas e saldos.</p>
      </div>

      {/* Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            {editing ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            {editing ? "Editar conta" : "Nova conta"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
              <div className="space-y-2">
                <Label>Nome</Label>
                <Input
                  value={form.name}
                  onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Tipo</Label>
                <Input
                  value={form.type}
                  onChange={(e) => setForm((prev) => ({ ...prev, type: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Moeda</Label>
                <Input
                  value={form.currency}
                  onChange={(e) => setForm((prev) => ({ ...prev, currency: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Saldo inicial (centavos)</Label>
                <Input
                  type="number"
                  min="0"
                  value={form.initialBalanceCents}
                  onChange={(e) => setForm((prev) => ({ ...prev, initialBalanceCents: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Instituicao</Label>
                <Select value={form.institutionId} onValueChange={(v) => setForm((prev) => ({ ...prev, institutionId: v }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue placeholder="Sem instituicao" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Sem instituicao</SelectItem>
                    {institutions.map((inst) => (
                      <SelectItem key={inst.id} value={String(inst.id)}>{inst.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="flex gap-2">
              <Button type="submit">Salvar</Button>
              {editing && (
                <Button type="button" variant="outline" onClick={cancelEdit}>Cancelar</Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      {/* List */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Lista de contas</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 w-full" />
              ))}
            </div>
          ) : accounts.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma conta cadastrada.</p>
          ) : (
            <div className="space-y-3">
              {accounts.map((account) => (
                <div
                  key={account.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10">
                      <Wallet className="h-6 w-6 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{account.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {account.type} · {account.currency}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        Instituicao: {institutions.find((inst) => inst.id === account.institutionId)?.name || "—"}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 sm:gap-6">
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">Saldo</p>
                      <p className={`text-lg font-bold ${account.balanceCents >= 0 ? "text-primary" : "text-destructive"}`}>
                        {formatCents(account.balanceCents)}
                      </p>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="icon" onClick={() => editAccount(account)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => deleteAccount(account.id)}>
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
