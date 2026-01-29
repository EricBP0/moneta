"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Building2, Pencil, Trash2, Plus } from "lucide-react"

interface Institution {
  id: number
  name: string
  type: string
}

const defaultForm = { name: "", type: "" }

export default function InstitutionsPage() {
  const { addToast } = useAppToast()
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [form, setForm] = useState(defaultForm)
  const [editing, setEditing] = useState<Institution | null>(null)
  const [loading, setLoading] = useState(true)

  const loadInstitutions = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiClient.get<Institution[]>("/api/institutions")
      setInstitutions(data || [])
    } catch {
      addToast("Erro ao carregar instituicoes.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadInstitutions()
  }, [loadInstitutions])

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault()
    try {
      const payload = { name: form.name, type: form.type }
      if (editing) {
        await apiClient.patch(`/api/institutions/${editing.id}`, payload)
        addToast("Instituicao atualizada.", "success")
      } else {
        await apiClient.post("/api/institutions", payload)
        addToast("Instituicao criada.", "success")
      }
      setForm(defaultForm)
      setEditing(null)
      loadInstitutions()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const editInstitution = (institution: Institution) => {
    setEditing(institution)
    setForm({ name: institution.name, type: institution.type || "" })
  }

  const deleteInstitution = async (institutionId: number) => {
    if (!window.confirm("Deseja desativar esta instituicao?")) return
    try {
      await apiClient.delete(`/api/institutions/${institutionId}`)
      addToast("Instituicao desativada.", "success")
      loadInstitutions()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Instituicoes</h1>
        <p className="text-muted-foreground">Bancos e provedores conectados.</p>
      </div>

      {/* Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            {editing ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            {editing ? "Editar instituicao" : "Nova instituicao"}
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
                <Label>Tipo</Label>
                <Input
                  value={form.type}
                  onChange={(e) => setForm((prev) => ({ ...prev, type: e.target.value }))}
                  placeholder="Banco, Corretora..."
                  className="bg-input border-border"
                />
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
          ) : institutions.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma instituicao cadastrada.</p>
          ) : (
            <div className="space-y-3">
              {institutions.map((institution) => (
                <div
                  key={institution.id}
                  className="flex items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                      <Building2 className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{institution.name}</p>
                      <p className="text-sm text-muted-foreground">{institution.type || "—"}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon" onClick={() => editInstitution(institution)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => deleteInstitution(institution.id)}>
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
