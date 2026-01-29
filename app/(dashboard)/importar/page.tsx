"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient, uploadFile } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Upload, FileText, CheckCircle, Clock, AlertCircle, RefreshCw } from "lucide-react"

interface Account {
  id: number
  name: string
}

interface ImportJob {
  id: number
  accountId: number
  filename: string
  format: string
  status: string
  rowsTotal: number
  rowsProcessed: number
  rowsFailed: number
  startedAt: string | null
  finishedAt: string | null
}

export default function ImportPage() {
  const { addToast } = useAppToast()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [jobs, setJobs] = useState<ImportJob[]>([])
  const [accountId, setAccountId] = useState("")
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [accountsData, jobsData] = await Promise.all([
        apiClient.get<Account[]>("/api/accounts"),
        apiClient.get<ImportJob[]>("/api/imports"),
      ])
      setAccounts(accountsData || [])
      setJobs(jobsData || [])
    } catch {
      addToast("Erro ao carregar dados.", "error")
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0]
    setFile(selectedFile || null)
  }

  const submitImport = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!file || !accountId) {
      addToast("Selecione conta e arquivo.", "error")
      return
    }
    setUploading(true)
    try {
      const ext = file.name.split(".").pop()?.toLowerCase()
      let format = "CSV"
      if (ext === "ofx") format = "OFX"
      else if (ext === "json") format = "JSON"
      await uploadFile("/api/imports/upload", file, {
        accountId: accountId,
        format: format,
        filename: file.name,
      })
      addToast("Importacao iniciada.", "success")
      setFile(null)
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    } finally {
      setUploading(false)
    }
  }

  const retryJob = async (jobId: number) => {
    try {
      await apiClient.post(`/api/imports/${jobId}/retry`, {})
      addToast("Retentativa iniciada.", "success")
      loadData()
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro"
      addToast(message, "error")
    }
  }

  const statusIcon = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return <CheckCircle className="h-4 w-4 text-primary" />
      case "PENDING":
      case "PROCESSING":
        return <Clock className="h-4 w-4 text-yellow-500" />
      case "FAILED":
        return <AlertCircle className="h-4 w-4 text-destructive" />
      default:
        return <FileText className="h-4 w-4 text-muted-foreground" />
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Importar</h1>
        <p className="text-muted-foreground">Carregue extratos CSV, OFX ou JSON.</p>
      </div>

      {/* Upload Form */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Upload className="h-5 w-5" />
            Upload de arquivo
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submitImport} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Conta</Label>
                <Select value={accountId} onValueChange={setAccountId}>
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
                <Label>Arquivo</Label>
                <Input
                  type="file"
                  accept=".csv,.ofx,.json"
                  onChange={handleFileSelect}
                  className="bg-input border-border"
                />
              </div>
            </div>
            <Button type="submit" disabled={uploading || !file || !accountId}>
              {uploading ? "Enviando..." : "Enviar"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* Jobs List */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Historico de importacoes</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 w-full" />
              ))}
            </div>
          ) : jobs.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhuma importacao registrada.</p>
          ) : (
            <div className="space-y-3">
              {jobs.map((job) => (
                <div
                  key={job.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                      <FileText className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{job.filename}</p>
                      <p className="text-sm text-muted-foreground">
                        Conta: {accounts.find((a) => a.id === job.accountId)?.name || job.accountId} · {job.format}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {job.rowsProcessed}/{job.rowsTotal} linhas · {job.rowsFailed} falhas
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2">
                      {statusIcon(job.status)}
                      <span className={`text-sm font-medium ${
                        job.status === "COMPLETED" ? "text-primary" :
                        job.status === "FAILED" ? "text-destructive" : "text-yellow-500"
                      }`}>
                        {job.status}
                      </span>
                    </div>
                    {job.status === "FAILED" && (
                      <Button variant="ghost" size="icon" onClick={() => retryJob(job.id)}>
                        <RefreshCw className="h-4 w-4" />
                      </Button>
                    )}
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
