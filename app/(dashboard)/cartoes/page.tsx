"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { formatCentsToInput, parseMoneyToCents } from "@/lib/utils/money"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Plus, Pencil, Trash2, CreditCard } from "lucide-react"

interface Account {
  id: number
  name: string
}

interface CreditCard {
  id: number
  accountId: number
  accountName: string
  name: string
  brand: string | null
  last4: string | null
  limitAmount: string
  closingDay: number
  dueDay: number
  isActive: boolean
}

const defaultCardForm = {
  accountId: undefined as string | undefined,
  name: "",
  brand: "",
  last4: "",
  limitAmount: "",
  closingDay: "",
  dueDay: "",
}

export default function CartoesPage() {
  const { addToast } = useAppToast()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<CreditCard[]>([])
  const [loading, setLoading] = useState(true)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editing, setEditing] = useState<CreditCard | null>(null)
  const [form, setForm] = useState(defaultCardForm)

  const loadAccounts = useCallback(async () => {
    try {
      const data = await apiClient.get<Account[]>("/api/accounts")
      setAccounts(data)
    } catch (error) {
      addToast({
        message: error instanceof Error ? error.message : "Erro ao carregar contas",
        type: "error",
      })
    }
  }, [addToast])

  const loadCards = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiClient.get<CreditCard[]>("/api/cards")
      setCards(data)
    } catch (error) {
      addToast({
        message: error instanceof Error ? error.message : "Erro ao carregar cartões",
        type: "error",
      })
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadAccounts()
    loadCards()
  }, [loadAccounts, loadCards])

  const openCreateForm = () => {
    setEditing(null)
    setForm(defaultCardForm)
    setIsFormOpen(true)
  }

  const openEditForm = (card: CreditCard) => {
    setEditing(card)
    setForm({
      accountId: String(card.accountId),
      name: card.name,
      brand: card.brand || "",
      last4: card.last4 || "",
      limitAmount: card.limitAmount, // Already in decimal format from backend
      closingDay: String(card.closingDay),
      dueDay: String(card.dueDay),
    })
    setIsFormOpen(true)
  }

  const closeForm = () => {
    setIsFormOpen(false)
    setEditing(null)
    setForm(defaultCardForm)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!form.accountId) {
      addToast({ message: "Selecione uma conta", type: "error" })
      return
    }

    if (!form.name.trim()) {
      addToast({ message: "Nome do cartão é obrigatório", type: "error" })
      return
    }

    if (!form.limitAmount || parseFloat(form.limitAmount) < 0) {
      addToast({ message: "Limite deve ser maior ou igual a zero", type: "error" })
      return
    }

    const closingDay = parseInt(form.closingDay)
    if (isNaN(closingDay) || closingDay < 1 || closingDay > 31) {
      addToast({ message: "Dia de fechamento deve estar entre 1 e 31", type: "error" })
      return
    }

    const dueDay = parseInt(form.dueDay)
    if (isNaN(dueDay) || dueDay < 1 || dueDay > 31) {
      addToast({ message: "Dia de vencimento deve estar entre 1 e 31", type: "error" })
      return
    }

    const limitAmountDecimal = parseFloat(form.limitAmount)

    const payload = {
      accountId: parseInt(form.accountId),
      name: form.name,
      brand: form.brand || null,
      last4: form.last4 || null,
      limitAmount: limitAmountDecimal,
      closingDay,
      dueDay,
    }

    try {
      if (editing) {
        await apiClient.patch(`/api/cards/${editing.id}`, payload)
        addToast({ message: "Cartão atualizado com sucesso", type: "success" })
      } else {
        await apiClient.post("/api/cards", payload)
        addToast({ message: "Cartão criado com sucesso", type: "success" })
      }
      closeForm()
      loadCards()
    } catch (error) {
      addToast({
        message: error instanceof Error ? error.message : "Erro ao salvar cartão",
        type: "error",
      })
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm("Deseja realmente excluir este cartão?")) return

    try {
      await apiClient.delete(`/api/cards/${id}`)
      addToast({ message: "Cartão excluído com sucesso", type: "success" })
      loadCards()
    } catch (error) {
      addToast({
        message: error instanceof Error ? error.message : "Erro ao excluir cartão",
        type: "error",
      })
    }
  }

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CreditCard className="h-6 w-6" />
            Cartões de Crédito
          </CardTitle>
          <Button onClick={openCreateForm}>
            <Plus className="mr-2 h-4 w-4" />
            Novo Cartão
          </Button>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : cards.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">
              Nenhum cartão cadastrado. Clique em &quot;Novo Cartão&quot; para adicionar.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Conta</TableHead>
                  <TableHead>Bandeira</TableHead>
                  <TableHead>Final</TableHead>
                  <TableHead>Limite</TableHead>
                  <TableHead>Fechamento</TableHead>
                  <TableHead>Vencimento</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {cards.map((card) => (
                  <TableRow key={card.id}>
                    <TableCell className="font-medium">{card.name}</TableCell>
                    <TableCell>{card.accountName}</TableCell>
                    <TableCell>{card.brand || "-"}</TableCell>
                    <TableCell>{card.last4 || "-"}</TableCell>
                    <TableCell>
                      R$ {parseFloat(card.limitAmount).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </TableCell>
                    <TableCell>Dia {card.closingDay}</TableCell>
                    <TableCell>Dia {card.dueDay}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex gap-2 justify-end">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openEditForm(card)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDelete(card.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={isFormOpen} onOpenChange={closeForm}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editing ? "Editar Cartão" : "Novo Cartão"}
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="accountId">Conta *</Label>
              <Select
                value={form.accountId}
                onValueChange={(value) => setForm({ ...form, accountId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Selecione a conta" />
                </SelectTrigger>
                <SelectContent>
                  {accounts.map((account) => (
                    <SelectItem key={account.id} value={String(account.id)}>
                      {account.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="name">Nome do Cartão *</Label>
              <Input
                id="name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Ex: Visa Gold"
                required
              />
            </div>

            <div>
              <Label htmlFor="brand">Bandeira</Label>
              <Input
                id="brand"
                value={form.brand}
                onChange={(e) => setForm({ ...form, brand: e.target.value })}
                placeholder="Ex: Visa, Mastercard, Elo"
              />
            </div>

            <div>
              <Label htmlFor="last4">Últimos 4 dígitos</Label>
              <Input
                id="last4"
                value={form.last4}
                onChange={(e) => setForm({ ...form, last4: e.target.value.slice(0, 4) })}
                placeholder="1234"
                maxLength={4}
              />
            </div>

            <div>
              <Label htmlFor="limitAmount">Limite (R$) *</Label>
              <Input
                id="limitAmount"
                value={form.limitAmount}
                onChange={(e) => setForm({ ...form, limitAmount: e.target.value })}
                placeholder="5000.00"
                type="number"
                step="0.01"
                min="0"
                required
              />
            </div>

            <div>
              <Label htmlFor="closingDay">Dia de Fechamento (1-31) *</Label>
              <Input
                id="closingDay"
                value={form.closingDay}
                onChange={(e) => setForm({ ...form, closingDay: e.target.value })}
                placeholder="15"
                type="number"
                min="1"
                max="31"
                required
              />
            </div>

            <div>
              <Label htmlFor="dueDay">Dia de Vencimento (1-31) *</Label>
              <Input
                id="dueDay"
                value={form.dueDay}
                onChange={(e) => setForm({ ...form, dueDay: e.target.value })}
                placeholder="10"
                type="number"
                min="1"
                max="31"
                required
              />
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={closeForm}>
                Cancelar
              </Button>
              <Button type="submit">
                {editing ? "Atualizar" : "Criar"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
