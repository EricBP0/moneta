"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { monthToday } from "@/lib/format"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import { Wand2, Plus, Pencil, Trash2, Play } from "lucide-react"

interface Category {
  id: number
  name: string
}

interface Account {
  id: number
  name: string
}

interface Rule {
  id: number
  name: string
  priority: number
  matchType: string
  pattern: string
  categoryId: number | null
  accountId: number | null
  isActive: boolean
}

const matchTypes = ["CONTAINS", "STARTS_WITH", "REGEX"]

const defaultForm = {
  name: "",
  priority: "0",
  matchType: "CONTAINS",
  pattern: "",
  categoryId: undefined as string | undefined,
  accountId: undefined as string | undefined,
  isActive: true,
}

export default function RulesPage() {
  const { addToast } = useAppToast()
  const [rules, setRules] = useState<Rule[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<Rule | null>(null)
  const [applyMonth, setApplyMonth] = useState(monthToday())
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [rulesData, categoriesData, accountsData] = await Promise.all([
        apiClient.get<Rule[]>("/api/rules"),
        apiClient.get<Category[]>("/api/categories"),
        apiClient.get<Account[]>("/api/accounts"),
      ])
      setRules(rulesData || [])
      setCategories(categoriesData || [])
      setAccounts(accountsData || [])
    } catch {
      addToast("Erro ao carregar regras.", "error")
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
        priority: Number(form.priority),
        matchType: form.matchType,
        pattern: form.pattern,
        categoryId: form.categoryId && form.categoryId !== "NONE" ? Number(form.categoryId) : null,
        subcategoryId: null,
        accountId: form.accountId && form.accountId !== "ALL" ? Number(form.accountId) : null,
        isActive: form.isActive,
      }
      if (editing) {
        await apiClient.patch(`/api/rules/${editing.id}`, payload)
        addToast("Regra atualizada.", "success")
      } else {
        await apiClient.post("/api/rules", payload)
        addToast("Regra criada.", "success")
      }
      setForm(defaultForm)
      setEditing(null)
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const editRule = (rule: Rule) => {
    setEditing(rule)
    setForm({
      name: rule.name,
      priority: String(rule.priority),
      matchType: rule.matchType,
      pattern: rule.pattern,
      categoryId: rule.categoryId ? String(rule.categoryId) : "NONE",
      accountId: rule.accountId ? String(rule.accountId) : "ALL",
      isActive: rule.isActive,
    })
  }

  const deleteRule = async (ruleId: number) => {
    if (!window.confirm("Deseja desativar esta regra?")) return
    try {
      await apiClient.delete(`/api/rules/${ruleId}`)
      addToast("Regra desativada.", "success")
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const applyRules = async () => {
    try {
      const payload = {
        month: applyMonth,
        accountId: null,
        onlyUncategorized: true,
        dryRun: false,
        overrideManual: false,
      }
      const response = await apiClient.post<{ evaluated: number; updated: number }>("/api/rules/apply", payload)
      addToast(`Avaliado ${response.evaluated}, atualizadas ${response.updated}.`, "success")
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Regras</h1>
        <p className="text-muted-foreground">Automatize categorizacoes.</p>
      </div>

      {/* Apply Rules */}
      <Card className="bg-card border-border">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Play className="h-5 w-5" />
            Aplicar regras
          </CardTitle>
          <Button onClick={applyRules}>
            <Wand2 className="mr-2 h-4 w-4" />
            Aplicar
          </Button>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2">
            <Label className="text-muted-foreground">Mes</Label>
            <Input
              type="month"
              value={applyMonth}
              onChange={(e) => setApplyMonth(e.target.value)}
              className="w-40 bg-input border-border"
            />
          </div>
        </CardContent>
      </Card>

      {/* Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            {editing ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            {editing ? "Editar regra" : "Nova regra"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
                <Label>Prioridade</Label>
                <Input
                  type="number"
                  min="0"
                  value={form.priority}
                  onChange={(e) => setForm((prev) => ({ ...prev, priority: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
              <div className="space-y-2">
                <Label>Match</Label>
                <Select value={form.matchType} onValueChange={(v) => setForm((prev) => ({ ...prev, matchType: v }))}>
                  <SelectTrigger className="bg-input border-border">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {matchTypes.map((t) => (
                      <SelectItem key={t} value={t}>{t}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Padrao</Label>
                <Input
                  value={form.pattern}
                  onChange={(e) => setForm((prev) => ({ ...prev, pattern: e.target.value }))}
                  required
                  className="bg-input border-border"
                />
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2">
                <Label>Categoria</Label>
                <Select
                  value={form.categoryId}
                  onValueChange={(v) => setForm((prev) => ({ ...prev, categoryId: v }))}
                >
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
                <Label>Conta</Label>
                <Select
                  value={form.accountId}
                  onValueChange={(v) => setForm((prev) => ({ ...prev, accountId: v }))}
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
              <div className="flex items-center gap-2 pt-6">
                <Checkbox
                  id="isActive"
                  checked={form.isActive}
                  onCheckedChange={(checked) => setForm((prev) => ({ ...prev, isActive: checked === true }))}
                />
                <Label htmlFor="isActive" className="cursor-pointer">Ativa</Label>
              </div>
            </div>
            <div className="flex gap-2">
              <Button type="submit">Salvar</Button>
              {editing && (
                <Button type="button" variant="outline" onClick={() => { setEditing(null); setForm(defaultForm) }}>
                  Cancelar
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      {/* List */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Lista de regras</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 w-full" />
              ))}
            </div>
          ) : rules.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma regra cadastrada.</p>
          ) : (
            <div className="space-y-3">
              {rules.map((rule) => (
                <div
                  key={rule.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                      <Wand2 className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{rule.name}</p>
                      <p className="text-sm text-muted-foreground">{rule.matchType} · {rule.pattern}</p>
                      <p className="text-xs text-muted-foreground">Prioridade {rule.priority}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                      rule.isActive ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
                    }`}>
                      {rule.isActive ? "Ativa" : "Inativa"}
                    </span>
                    <Button variant="ghost" size="icon" onClick={() => editRule(rule)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => deleteRule(rule.id)}>
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
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
