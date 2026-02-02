"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { formatCents, monthToday, toIsoDateTime } from "@/lib/format"
import { parseMoneyToCents, formatCentsToInput } from "@/lib/utils/money"
import { TRANSACTION_STATUS_OPTIONS, getTransactionStatusLabel } from "@/lib/constants/labels"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Plus, ArrowLeftRight, Pencil, Trash2, Wand2 } from "lucide-react"

interface Account {
  id: number
  name: string
}

interface Card {
  id: number
  name: string
}

interface Category {
  id: number
  name: string
  color: string | null
}

interface Transaction {
  id: number
  accountId: number | null
  cardId: number | null
  paymentType: "PIX" | "CARD"
  amountCents: number
  direction: "IN" | "OUT"
  description: string
  occurredAt: string
  status: "CLEARED" | "PENDING"
  categoryId: number | null
}

const defaultTxnForm = {
  paymentType: "PIX" as "PIX" | "CARD",
  accountId: undefined as string | undefined,
  cardId: undefined as string | undefined,
  amountCents: "",
  direction: "OUT" as "IN" | "OUT",
  description: "",
  occurredAt: "",
  status: "CLEARED" as "CLEARED" | "PENDING",
  categoryId: "NONE" as string,
}

const defaultTransferForm = {
  fromAccountId: undefined as string | undefined,
  toAccountId: undefined as string | undefined,
  amountCents: "",
  occurredAt: "",
  description: "",
}

export default function TransactionsPage() {
  const { addToast } = useAppToast()
  const [filters, setFilters] = useState({
    month: monthToday(),
    accountId: "ALL",
    categoryId: "ALL",
    query: "",
    direction: "ALL",
    status: "ALL",
  })
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [txns, setTxns] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editing, setEditing] = useState<Transaction | null>(null)
  const [form, setForm] = useState(defaultTxnForm)
  const [transferOpen, setTransferOpen] = useState(false)
  const [transferForm, setTransferForm] = useState(defaultTransferForm)

  const filterQuery = useMemo(() => {
    const params = new URLSearchParams()
    if (filters.month) params.append("month", filters.month)
    if (filters.accountId && filters.accountId !== "ALL") params.append("accountId", filters.accountId)
    if (filters.categoryId && filters.categoryId !== "ALL") params.append("categoryId", filters.categoryId)
    if (filters.query) params.append("q", filters.query)
    if (filters.direction && filters.direction !== "ALL") params.append("direction", filters.direction)
    if (filters.status && filters.status !== "ALL") params.append("status", filters.status)
    return params.toString()
  }, [filters])

  const loadSupporting = useCallback(async () => {
    const [accountsData, categoriesData, cardsData] = await Promise.all([
      apiClient.get<Account[]>("/api/accounts"),
      apiClient.get<Category[]>("/api/categories"),
      apiClient.get<Card[]>("/api/cards"),
    ])
    setAccounts(accountsData)
    setCategories(categoriesData)
    setCards(cardsData)
  }, [])

  const loadTxns = useCallback(async () => {
    setLoading(true)
    try {
      const response = await apiClient.get<Transaction[]>(`/api/txns?${filterQuery}`)
      setTxns(response || [])
    } catch (err) {
      addToast("Erro ao carregar transacoes.", "error")
    } finally {
      setLoading(false)
    }
  }, [filterQuery, addToast])

  useEffect(() => {
    loadSupporting()
  }, [loadSupporting])

  useEffect(() => {
    loadTxns()
  }, [loadTxns])

  const openCreate = () => {
    setEditing(null)
    setForm(defaultTxnForm)
    setIsFormOpen(true)
  }

  const openEdit = (txn: Transaction) => {
    setEditing(txn)
    setForm({
      paymentType: txn.paymentType || "PIX",
      accountId: txn.accountId ? String(txn.accountId) : undefined,
      cardId: txn.cardId ? String(txn.cardId) : undefined,
      amountCents: formatCentsToInput(txn.amountCents),
      direction: txn.direction,
      description: txn.description || "",
      occurredAt: txn.occurredAt ? new Date(txn.occurredAt).toISOString().slice(0, 16) : "",
      status: txn.status || "CLEARED",
      categoryId: txn.categoryId ? String(txn.categoryId) : "NONE",
    })
    setIsFormOpen(true)
  }

  const submitTxn = async (event: React.FormEvent) => {
    event.preventDefault()
    if (form.paymentType === "PIX" && !form.accountId) {
      addToast("Selecione uma conta para a transacao.", "error")
      return
    }
    if (form.paymentType === "CARD" && !form.cardId) {
      addToast("Selecione um cartao para a transacao.", "error")
      return
    }
    try {
      const payload = {
        paymentType: form.paymentType,
        accountId: form.paymentType === "PIX" ? Number(form.accountId) : null,
        cardId: form.paymentType === "CARD" ? Number(form.cardId) : null,
        amountCents: parseMoneyToCents(form.amountCents),
        direction: form.direction,
        description: form.description,
        occurredAt: toIsoDateTime(form.occurredAt),
        status: form.status,
        categoryId: form.categoryId && form.categoryId !== "NONE" ? Number(form.categoryId) : null,
      }
      if (editing) {
        await apiClient.patch(`/api/txns/${editing.id}`, payload)
        addToast("Transacao atualizada.", "success")
      } else {
        await apiClient.post("/api/txns", payload)
        addToast("Transacao criada.", "success")
      }
      setIsFormOpen(false)
      loadTxns()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const deleteTxn = async (txnId: number) => {
    if (!window.confirm("Deseja remover esta transacao?")) return
    try {
      await apiClient.delete(`/api/txns/${txnId}`)
      addToast("Transacao removida.", "success")
      loadTxns()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const submitTransfer = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!transferForm.fromAccountId || !transferForm.toAccountId) {
      addToast("Selecione as contas de origem e destino.", "error")
      return
    }
    if (transferForm.fromAccountId === transferForm.toAccountId) {
      addToast("As contas de origem e destino devem ser diferentes.", "error")
      return
    }
    try {
      const payload = {
        fromAccountId: Number(transferForm.fromAccountId),
        toAccountId: Number(transferForm.toAccountId),
        amountCents: parseMoneyToCents(transferForm.amountCents),
        occurredAt: toIsoDateTime(transferForm.occurredAt),
        description: transferForm.description,
      }
      await apiClient.post("/api/txns/transfer", payload)
      addToast("Transferencia registrada.", "success")
      setTransferOpen(false)
      setTransferForm(defaultTransferForm)
      loadTxns()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const applyRules = async () => {
    try {
      const payload = {
        month: filters.month,
        accountId: filters.accountId && filters.accountId !== "ALL" ? Number(filters.accountId) : null,
        onlyUncategorized: true,
        dryRun: false,
        overrideManual: false,
      }
      const response = await apiClient.post<{ updated: number }>("/api/rules/apply", payload)
      addToast(`Regras aplicadas: ${response.updated} atualizadas.`, "success")
      loadTxns()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Transacoes</h1>
          <p className="text-muted-foreground">Filtre e gerencie lancamentos.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setTransferOpen(true)}>
            <ArrowLeftRight className="mr-2 h-4 w-4" />
            Transferencia
          </Button>
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            Nova transacao
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Filtros</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
            <div className="space-y-2">
              <Label>Mes</Label>
              <Input
                type="month"
                value={filters.month}
                onChange={(e) => setFilters((prev) => ({ ...prev, month: e.target.value }))}
                className="bg-input border-border"
              />
            </div>
            <div className="space-y-2">
              <Label>Conta</Label>
              <Select
                value={filters.accountId}
                onValueChange={(v) => setFilters((prev) => ({ ...prev, accountId: v }))}
              >
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Todas" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Todas</SelectItem>
                  {accounts.map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Categoria</Label>
              <Select
                value={filters.categoryId}
                onValueChange={(v) => setFilters((prev) => ({ ...prev, categoryId: v }))}
              >
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Todas" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Todas</SelectItem>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Texto</Label>
              <Input
                value={filters.query}
                onChange={(e) => setFilters((prev) => ({ ...prev, query: e.target.value }))}
                placeholder="Buscar..."
                className="bg-input border-border"
              />
            </div>
            <div className="space-y-2">
              <Label>Direcao</Label>
              <Select
                value={filters.direction}
                onValueChange={(v) => setFilters((prev) => ({ ...prev, direction: v }))}
              >
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Todas" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Todas</SelectItem>
                  <SelectItem value="IN">Entrada</SelectItem>
                  <SelectItem value="OUT">Saida</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Status</Label>
              <Select
                value={filters.status}
                onValueChange={(v) => setFilters((prev) => ({ ...prev, status: v }))}
              >
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Todos" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Todos</SelectItem>
                  {TRANSACTION_STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="mt-4">
            <Button variant="outline" onClick={applyRules}>
              <Wand2 className="mr-2 h-4 w-4" />
              Aplicar regras
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Lista</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : txns.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma transacao encontrada.</p>
          ) : (
            <>
              {/* Desktop table view */}
              <div className="hidden md:block overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-border">
                      <TableHead>Data</TableHead>
                      <TableHead>Conta/Cartão</TableHead>
                      <TableHead>Descricao</TableHead>
                      <TableHead>Categoria</TableHead>
                      <TableHead>Direcao</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Valor</TableHead>
                      <TableHead className="w-24"></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {txns.map((txn) => {
                      const category = txn.categoryId ? categories.find((c) => c.id === txn.categoryId) : null;
                      const account = txn.accountId ? accounts.find((a) => a.id === txn.accountId) : undefined;
                      const card = txn.cardId ? cards.find((c) => c.id === txn.cardId) : undefined;
                      const paymentLabel = txn.paymentType === "CARD"
                        ? `Cartão: ${card?.name || txn.cardId || "—"}`
                        : account?.name || txn.accountId || "—";
                      return (
                        <TableRow key={txn.id} className="border-border">
                          <TableCell>{new Date(txn.occurredAt).toLocaleDateString("pt-BR")}</TableCell>
                          <TableCell>{paymentLabel}</TableCell>
                          <TableCell>{txn.description || "—"}</TableCell>
                          <TableCell>
                            {category ? (
                              <div className="flex items-center gap-2">
                                <div
                                  className="w-3 h-3 rounded-full flex-shrink-0"
                                  style={{ backgroundColor: category.color || "#6b7280" }}
                                />
                                <span>{category.name || "—"}</span>
                              </div>
                            ) : (
                              <span className="text-muted-foreground">—</span>
                            )}
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                              txn.direction === "IN" ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive"
                            }`}>
                              {txn.direction === "IN" ? "Entrada" : "Saida"}
                            </span>
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                              txn.status === "CLEARED" ? "bg-primary/10 text-primary" : "bg-yellow-500/10 text-yellow-500"
                            }`}>
                              {getTransactionStatusLabel(txn.status)}
                            </span>
                          </TableCell>
                          <TableCell className={`text-right font-medium ${
                            txn.direction === "IN" ? "text-primary" : "text-destructive"
                          }`}>
                            {txn.direction === "IN" ? "+" : "-"}{formatCents(txn.amountCents)}
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-1">
                              <Button variant="ghost" size="icon" onClick={() => openEdit(txn)}>
                                <Pencil className="h-4 w-4" />
                              </Button>
                              <Button variant="ghost" size="icon" onClick={() => deleteTxn(txn.id)}>
                                <Trash2 className="h-4 w-4 text-destructive" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </div>
              
              {/* Mobile card view */}
              <div className="md:hidden space-y-3">
                {txns.map((txn) => {
                  const category = txn.categoryId ? categories.find((c) => c.id === txn.categoryId) : null;
                  const account = txn.accountId ? accounts.find((a) => a.id === txn.accountId) : undefined;
                  const card = txn.cardId ? cards.find((c) => c.id === txn.cardId) : undefined;
                  const paymentLabel = txn.paymentType === "CARD"
                    ? `Cartão: ${card?.name || txn.cardId || "—"}`
                    : account?.name || txn.accountId || "—";
                  return (
                    <div
                      key={txn.id}
                      className="p-4 rounded-lg bg-secondary/50 border border-border space-y-3"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-foreground truncate">
                            {txn.description || "Sem descrição"}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            {paymentLabel}
                          </p>
                        </div>
                        <div className={`text-right font-bold ${
                          txn.direction === "IN" ? "text-primary" : "text-destructive"
                        }`}>
                          {txn.direction === "IN" ? "+" : "-"}{formatCents(txn.amountCents)}
                        </div>
                      </div>
                      
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="text-muted-foreground">
                          {new Date(txn.occurredAt).toLocaleDateString("pt-BR")}
                        </span>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full font-medium ${
                          txn.direction === "IN" ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive"
                        }`}>
                          {txn.direction === "IN" ? "Entrada" : "Saida"}
                        </span>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full font-medium ${
                          txn.status === "CLEARED" ? "bg-primary/10 text-primary" : "bg-yellow-500/10 text-yellow-500"
                        }`}>
                          {getTransactionStatusLabel(txn.status)}
                        </span>
                      </div>
                      
                      {category && (
                        <div className="flex items-center gap-2 text-sm">
                          <div
                            className="w-3 h-3 rounded-full flex-shrink-0"
                            style={{ backgroundColor: category.color || "#6b7280" }}
                          />
                          <span className="text-muted-foreground">{category.name}</span>
                        </div>
                      )}
                      
                      <div className="flex items-center gap-2 pt-2 border-t border-border">
                        <Button variant="outline" size="sm" onClick={() => openEdit(txn)} className="flex-1">
                          <Pencil className="h-4 w-4 mr-2" />
                          Editar
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => deleteTxn(txn.id)} className="text-destructive">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Transaction Dialog */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Editar transacao" : "Nova transacao"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={submitTxn} className="space-y-4">
            <div className="space-y-2">
              <Label>Tipo</Label>
              <Select
                value={form.paymentType}
                onValueChange={(value) => setForm((prev) => ({
                  ...prev,
                  paymentType: value as "PIX" | "CARD",
                  accountId: value === "PIX" ? prev.accountId : undefined,
                  cardId: value === "CARD" ? prev.cardId : undefined,
                }))}
              >
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PIX">PIX</SelectItem>
                  <SelectItem value="CARD">Cartão</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {form.paymentType === "PIX" ? (
              <div className="space-y-2">
                <Label>Conta</Label>
                <Select value={form.accountId} onValueChange={(v) => setForm((prev) => ({ ...prev, accountId: v }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts.map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div className="space-y-2">
                <Label>Cartão</Label>
                <Select value={form.cardId} onValueChange={(v) => setForm((prev) => ({ ...prev, cardId: v }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    {cards.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Valor</Label>
                <Input
                  type="text"
                  inputMode="decimal"
                  value={form.amountCents}
                  onChange={(e) => setForm((prev) => ({ ...prev, amountCents: e.target.value }))}
                  placeholder="0,00"
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Direcao</Label>
                <Select value={form.direction} onValueChange={(v) => setForm((prev) => ({ ...prev, direction: v as "IN" | "OUT" }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="IN">Entrada</SelectItem>
                    <SelectItem value="OUT">Saida</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Data/Hora</Label>
              <Input
                type="datetime-local"
                value={form.occurredAt}
                onChange={(e) => setForm((prev) => ({ ...prev, occurredAt: e.target.value }))}
                required
                className="bg-input border-border"
              />
            </div>
            <div className="space-y-2">
              <Label>Descricao</Label>
              <Input
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                className="bg-input border-border"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Categoria</Label>
              <Select value={form.categoryId} onValueChange={(v) => setForm((prev) => ({ ...prev, categoryId: v }))}>
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Sem categoria" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">Sem categoria</SelectItem>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Status</Label>
                <Select value={form.status} onValueChange={(v) => setForm((prev) => ({ ...prev, status: v as "CLEARED" | "PENDING" }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {TRANSACTION_STATUS_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>Cancelar</Button>
              <Button type="submit">Salvar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Transfer Dialog */}
      <Dialog open={transferOpen} onOpenChange={setTransferOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Transferencia</DialogTitle>
          </DialogHeader>
          <form onSubmit={submitTransfer} className="space-y-4">
            <div className="space-y-2">
              <Label>Conta origem</Label>
              <Select value={transferForm.fromAccountId} onValueChange={(v) => setTransferForm((prev) => ({ ...prev, fromAccountId: v }))}>
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  {accounts.map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Conta destino</Label>
              <Select value={transferForm.toAccountId} onValueChange={(v) => setTransferForm((prev) => ({ ...prev, toAccountId: v }))}>
                <SelectTrigger className="bg-input border-border">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  {accounts.map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Valor</Label>
              <Input
                type="text"
                inputMode="decimal"
                value={transferForm.amountCents}
                onChange={(e) => setTransferForm((prev) => ({ ...prev, amountCents: e.target.value }))}
                placeholder="0,00"
                required
                className="bg-input border-border"
              />
            </div>
            <div className="space-y-2">
              <Label>Data/Hora</Label>
              <Input
                type="datetime-local"
                value={transferForm.occurredAt}
                onChange={(e) => setTransferForm((prev) => ({ ...prev, occurredAt: e.target.value }))}
                required
                className="bg-input border-border"
              />
            </div>
            <div className="space-y-2">
              <Label>Descricao</Label>
              <Input
                value={transferForm.description}
                onChange={(e) => setTransferForm((prev) => ({ ...prev, description: e.target.value }))}
                className="bg-input border-border"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setTransferOpen(false)}>Cancelar</Button>
              <Button type="submit">Confirmar</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
