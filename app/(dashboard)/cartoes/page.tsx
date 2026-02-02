"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { formatCents } from "@/lib/format"
import { parseMoneyToCents, formatCentsToInput } from "@/lib/utils/money"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { CreditCard, Pencil, Trash2, Plus } from "lucide-react"

interface Account {
  id: number
  name: string
}

interface CardItem {
  id: number
  accountId: number
  name: string
  brand: string | null
  last4: string | null
  limitAmount: number
  closingDay: number
  dueDay: number
  active: boolean
}

const defaultForm = {
  name: "",
  accountId: "NONE",
  brand: "",
  last4: "",
  limitAmount: "",
  closingDay: "10",
  dueDay: "17",
}

export default function CardsPage() {
  const { addToast } = useAppToast()
  const [cards, setCards] = useState<CardItem[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<CardItem | null>(null)
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [cardsData, accountsData] = await Promise.all([
        apiClient.get<CardItem[]>("/api/cards"),
        apiClient.get<Account[]>("/api/accounts"),
      ])
      setCards(cardsData)
      setAccounts(accountsData)
    } catch (err) {
      addToast("Erro ao carregar cartoes.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!form.accountId || form.accountId === "NONE") {
      addToast("Selecione uma conta vinculada.", "error")
      return
    }
    try {
      const limitAmount = parseMoneyToCents(form.limitAmount)
      const payload = {
        name: form.name,
        accountId: Number(form.accountId),
        brand: form.brand || null,
        last4: form.last4 || null,
        limitAmount: limitAmount / 100,
        closingDay: Number(form.closingDay),
        dueDay: Number(form.dueDay),
      }
      if (editing) {
        await apiClient.put(`/api/cards/${editing.id}`, payload)
        addToast("Cartao atualizado.", "success")
      } else {
        await apiClient.post("/api/cards", payload)
        addToast("Cartao criado.", "success")
      }
      setForm(defaultForm)
      setEditing(null)
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const editCard = (card: CardItem) => {
    setEditing(card)
    const limitCents = Math.round((card.limitAmount || 0) * 100)
    setForm({
      name: card.name,
      accountId: String(card.accountId),
      brand: card.brand || "",
      last4: card.last4 || "",
      limitAmount: formatCentsToInput(limitCents),
      closingDay: String(card.closingDay),
      dueDay: String(card.dueDay),
    })
  }

  const deleteCard = async (cardId: number) => {
    if (!window.confirm("Deseja desativar este cartao?")) return
    try {
      await apiClient.delete(`/api/cards/${cardId}`)
      addToast("Cartao desativado.", "success")
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
        <h1 className="text-2xl font-bold text-foreground">Cartoes</h1>
        <p className="text-muted-foreground">Gerencie seus cartoes de credito.</p>
      </div>

      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            {editing ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            {editing ? "Editar cartao" : "Novo cartao"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
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
                <Label>Conta vinculada</Label>
                <Select
                  value={form.accountId}
                  onValueChange={(value) => setForm((prev) => ({ ...prev, accountId: value }))}
                >
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">Selecione</SelectItem>
                    {accounts.map((account) => (
                      <SelectItem key={account.id} value={String(account.id)}>
                        {account.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Limite</Label>
                <Input
                  type="text"
                  inputMode="decimal"
                  value={form.limitAmount}
                  onChange={(e) => setForm((prev) => ({ ...prev, limitAmount: e.target.value }))}
                  placeholder="0,00"
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Fechamento</Label>
                <Input
                  type="number"
                  min={1}
                  max={31}
                  value={form.closingDay}
                  onChange={(e) => setForm((prev) => ({ ...prev, closingDay: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Vencimento</Label>
                <Input
                  type="number"
                  min={1}
                  max={31}
                  value={form.dueDay}
                  onChange={(e) => setForm((prev) => ({ ...prev, dueDay: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Bandeira</Label>
                <Input
                  value={form.brand}
                  onChange={(e) => setForm((prev) => ({ ...prev, brand: e.target.value }))}
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Final</Label>
                <Input
                  value={form.last4}
                  onChange={(e) => setForm((prev) => ({ ...prev, last4: e.target.value }))}
                  maxLength={4}
                  className="bg-input border-border"
                />
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button type="submit">Salvar</Button>
              {editing && (
                <Button type="button" variant="outline" onClick={cancelEdit}>
                  Cancelar
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <CreditCard className="h-5 w-5" />
            Cartoes cadastrados
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : cards.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhum cartao cadastrado.</p>
          ) : (
            <div className="space-y-3">
              {cards.map((card) => {
                const account = accounts.find((a) => a.id === card.accountId)
                const limitCents = Math.round((card.limitAmount || 0) * 100)
                return (
                  <div
                    key={card.id}
                    className="flex flex-col gap-3 rounded-lg border border-border bg-secondary/30 p-4 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="space-y-1">
                      <p className="font-medium text-foreground">{card.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {account?.name || "Conta não encontrada"} · Fechamento {card.closingDay} · Vencimento {card.dueDay}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        Limite: {formatCents(limitCents)}
                        {card.brand ? ` · ${card.brand}` : ""}{card.last4 ? ` · ${card.last4}` : ""}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button variant="outline" size="sm" onClick={() => editCard(card)}>
                        <Pencil className="mr-2 h-4 w-4" />
                        Editar
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => deleteCard(card.id)} className="text-destructive">
                        <Trash2 className="mr-2 h-4 w-4" />
                        Desativar
                      </Button>
                    </div>
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
