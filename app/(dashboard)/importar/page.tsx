"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api-client"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Upload, FileText, CheckCircle, Clock, AlertCircle, RefreshCw, Download, HelpCircle, Info } from "lucide-react"

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
  const [filePreview, setFilePreview] = useState<string>("")
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

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0]
    setFile(selectedFile || null)
    setFilePreview("")

    if (selectedFile) {
      const ext = selectedFile.name.split(".").pop()?.toLowerCase()
      
      // Show preview for CSV files
      if (ext === "csv") {
        try {
          const text = await selectedFile.text()
          const lines = text.split("\n").slice(0, 6) // First 6 lines (header + 5 rows)
          setFilePreview(lines.join("\n"))
        } catch (err) {
          console.error("Error reading file:", err)
        }
      } else {
        setFilePreview(`Arquivo ${ext?.toUpperCase()} selecionado: ${selectedFile.name}`)
      }
    }
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
      
      const formData = new FormData()
      formData.append('file', file)
      formData.append('accountId', accountId)
      formData.append('format', format)
      formData.append('filename', file.name)
      
      await apiClient.post("/api/imports/upload", formData, { isForm: true })
      
      addToast("Importação iniciada.", "success")
      setFile(null)
      setFilePreview("")
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

  const downloadTemplate = () => {
    const csvContent = [
      "data,descricao,valor,tipo",
      "2024-01-15,Supermercado Exemplo,15000,OUT",
      "2024-01-16,Salario,500000,IN",
      "2024-01-17,Restaurante,8500,OUT",
    ].join("\n")

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" })
    const link = document.createElement("a")
    const url = URL.createObjectURL(blob)
    
    link.setAttribute("href", url)
    link.setAttribute("download", "modelo-transacoes.csv")
    link.style.visibility = "hidden"
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    addToast("Modelo CSV baixado.", "success")
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

      {/* Instructions & Template */}
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <HelpCircle className="h-5 w-5 text-primary" />
            Como importar transações
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h3 className="font-medium text-foreground mb-2">Formatos aceitos</h3>
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="p-3 rounded-lg bg-secondary/50 border border-border">
                <p className="font-medium text-foreground mb-1">CSV</p>
                <p className="text-xs text-muted-foreground">
                  Arquivo de texto com colunas: data, descricao, valor, tipo
                </p>
              </div>
              <div className="p-3 rounded-lg bg-secondary/50 border border-border">
                <p className="font-medium text-foreground mb-1">OFX</p>
                <p className="text-xs text-muted-foreground">
                  Formato padrão de extratos bancários
                </p>
              </div>
              <div className="p-3 rounded-lg bg-secondary/50 border border-border">
                <p className="font-medium text-foreground mb-1">JSON</p>
                <p className="text-xs text-muted-foreground">
                  Formato estruturado para integração
                </p>
              </div>
            </div>
          </div>

          <div>
            <h3 className="font-medium text-foreground mb-2">Formato CSV esperado</h3>
            <div className="p-3 rounded-lg bg-secondary border border-border font-mono text-sm">
              <p className="text-muted-foreground mb-2"># Cabeçalho (obrigatório)</p>
              <p className="text-foreground">data,descricao,valor,tipo</p>
              <p className="text-muted-foreground mt-2 mb-2"># Exemplos de linhas</p>
              <p className="text-foreground">2024-01-15,Supermercado Exemplo,15000,OUT</p>
              <p className="text-foreground">2024-01-16,Salario,500000,IN</p>
            </div>
            <div className="mt-3 space-y-2 text-sm">
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">data:</strong>
                  <span className="text-muted-foreground"> Formato YYYY-MM-DD (ex: 2024-01-15)</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">descricao:</strong>
                  <span className="text-muted-foreground"> Texto descritivo da transação</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">valor:</strong>
                  <span className="text-muted-foreground"> Valor em centavos (ex: 15000 = R$ 150,00)</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">tipo:</strong>
                  <span className="text-muted-foreground"> IN (entrada) ou OUT (saída)</span>
                </div>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-border">
            <p className="text-sm text-muted-foreground">
              Baixe um modelo CSV pronto para preencher com suas transações
            </p>
            <Button onClick={downloadTemplate} variant="outline">
              <Download className="h-4 w-4 mr-2" />
              Baixar Modelo
            </Button>
          </div>
        </CardContent>
      </Card>

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

          {/* File Preview */}
          {filePreview && (
            <div className="mt-4 pt-4 border-t border-border">
              <h3 className="font-medium text-foreground mb-2 flex items-center gap-2">
                <FileText className="h-4 w-4 text-primary" />
                Preview do arquivo
              </h3>
              <div className="p-3 rounded-lg bg-secondary border border-border">
                <pre className="text-xs font-mono text-foreground whitespace-pre-wrap overflow-x-auto">
                  {filePreview}
                </pre>
              </div>
              <p className="text-xs text-muted-foreground mt-2">
                Primeiras linhas do arquivo. Verifique se o formato está correto antes de enviar.
              </p>
            </div>
          )}
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
