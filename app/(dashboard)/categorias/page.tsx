"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Pencil, Trash2, Plus } from "lucide-react"

interface Category {
  id: number
  name: string
  color: string | null
}

const defaultForm = { name: "", color: "" }

export default function CategoriesPage() {
  const { addToast } = useAppToast()
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<Category | null>(null)
  const [loading, setLoading] = useState(true)

  const loadCategories = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiClient.get<Category[]>("/api/categories")
      setCategories(data || [])
    } catch {
      addToast("Erro ao carregar categorias.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadCategories()
  }, [loadCategories])

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    try {
      const payload = { name: form.name, color: form.color || null }
      if (editing) {
        await apiClient.patch(`/api/categories/${editing.id}`, payload)
        addToast("Categoria atualizada.", "success")
      } else {
        await apiClient.post("/api/categories", payload)
        addToast("Categoria criada.", "success")
      }
      setForm(defaultForm)
      setEditing(null)
      loadCategories()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const editCategory = (category: Category) => {
    setEditing(category)
    setForm({ name: category.name, color: category.color || "" })
  }

  const deleteCategory = async (categoryId: number) => {
    if (!window.confirm("Deseja desativar esta categoria?")) return
    try {
      await apiClient.delete(`/api/categories/${categoryId}`)
      addToast("Categoria desativada.", "success")
      loadCategories()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Categorias</h1>
        <p className="text-muted-foreground">Organize gastos e receitas.</p>
      </div>

      {/* Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            {editing ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            {editing ? "Editar categoria" : "Nova categoria"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitForm} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
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
                <Label>Cor</Label>
                <div className="flex gap-2">
                  <Input
                    value={form.color}
                    onChange={(e) => setForm((prev) => ({ ...prev, color: e.target.value }))}
                    placeholder="#22c55e"
                    className="bg-input border-border flex-1"
                  />
                  <Input
                    type="color"
                    value={form.color || "#22c55e"}
                    onChange={(e) => setForm((prev) => ({ ...prev, color: e.target.value }))}
                    className="w-12 h-10 p-1 bg-input border-border cursor-pointer"
                  />
                </div>
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
          <CardTitle className="text-lg">Lista</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : categories.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma categoria cadastrada.</p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {categories.map((category) => (
                <div
                  key={category.id}
                  className="flex items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-3">
                    <div
                      className="w-4 h-4 rounded-full"
                      style={{ backgroundColor: category.color || "#22c55e" }}
                    />
                    <div>
                      <p className="font-medium text-foreground">{category.name}</p>
                      <p className="text-xs text-muted-foreground">{category.color || "Sem cor"}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon" onClick={() => editCategory(category)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => deleteCategory(category.id)}>
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
