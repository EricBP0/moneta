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
import { Upload, FileText, CheckCircle, Clock, AlertCircle, Download, HelpCircle, Info } from "lucide-react"

interface Account {
  id: number
  name: string
}

interface ImportJob {
  batchId: number
  accountId: number
  filename: string
  uploadedAt: string | null
  status: string
  totals: {
    totalRows: number
    errorRows: number
    duplicateRows: number
    readyRows: number
    committedRows: number
  }
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
        apiClient.get<ImportJob[]>("/api/import/batches"),
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
          // Limit preview to small files (max 1MB)
          if (selectedFile.size > 1024 * 1024) {
            setFilePreview(`Arquivo CSV selecionado: ${selectedFile.name} (muito grande para preview)`)
            return
          }
          
          const text = await selectedFile.text()
          const lines = text.split("\n").slice(0, 6) // First 6 lines (header + 5 rows)
          setFilePreview(lines.join("\n"))
        } catch (err) {
          console.error("Error reading file:", err)
        }
      } else {
        if (ext) {
          setFilePreview(`Arquivo ${ext.toUpperCase()} selecionado: ${selectedFile.name}`)
        } else {
          setFilePreview(`Arquivo selecionado: ${selectedFile.name}`)
        }
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
      const formData = new FormData()
      formData.append('file', file)
      formData.append('accountId', accountId)
      
      await apiClient.post("/api/import/csv", formData, { isForm: true })
      
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

  const downloadTemplate = () => {
    const csvContent = [
      "date,description,amount,payment_method,account,card,category",
      "2024-01-15,Supermercado,-150.50,PIX,Conta Corrente,,Alimentação",
      "2024-01-16,Posto de gasolina,-200.00,CARD,,Cartão Nubank,Transporte",
      "2024-01-17,Salário,5000.00,PIX,Conta Corrente,,Receita",
      "2024-01-18,Farmácia,-45.80,CARD,,Cartão Nubank,Saúde",
    ].join("\n")

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" })
    const link = document.createElement("a")
    const url = URL.createObjectURL(blob)
    
    link.setAttribute("href", url)
    link.setAttribute("download", "modelo-transacoes.csv")
    link.style.visibility = "hidden"
    document.body.appendChild(link)
    
    try {
      link.click()
    } finally {
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }
    
    addToast("Modelo CSV baixado.", "success")
  }

  const statusIcon = (status: string) => {
    switch (status) {
      case "COMMITTED":
        return <CheckCircle className="h-4 w-4 text-primary" />
      case "UPLOADED":
      case "PARSED":
        return <Clock className="h-4 w-4 text-yellow-500" />
      case "FAILED":
        return <AlertCircle className="h-4 w-4 text-destructive" />
      default:
        return <FileText className="h-4 w-4 text-muted-foreground" />
    }
  }

  const statusLabel = (status: string) => {
    switch (status) {
      case "COMMITTED":
        return "Concluída"
      case "UPLOADED":
        return "Enviada"
      case "PARSED":
        return "Validada"
      case "FAILED":
        return "Falhou"
      default:
        return status
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Importar</h1>
        <p className="text-muted-foreground">Carregue extratos CSV.</p>
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
            <h3 className="font-medium text-foreground mb-2">Formato aceito</h3>
            <div className="p-3 rounded-lg bg-secondary/50 border border-border">
              <p className="font-medium text-foreground mb-1">CSV</p>
              <p className="text-xs text-muted-foreground">
                Arquivo de texto com transações PIX e/ou CARTÃO
              </p>
            </div>
          </div>

          <div>
            <h3 className="font-medium text-foreground mb-2">Formato CSV esperado</h3>
            <div className="p-3 rounded-lg bg-secondary border border-border font-mono text-xs overflow-x-auto">
              <p className="text-muted-foreground mb-2"># Cabeçalho (obrigatório)</p>
              <p className="text-foreground whitespace-nowrap">date,description,amount,payment_method,account,card,category</p>
              <p className="text-muted-foreground mt-2 mb-2"># Exemplos de linhas</p>
              <p className="text-foreground whitespace-nowrap">2024-01-15,Supermercado,-150.50,PIX,Conta Corrente,,Alimentação</p>
              <p className="text-foreground whitespace-nowrap">2024-01-16,Posto de gasolina,-200.00,CARD,,Cartão Nubank,Transporte</p>
              <p className="text-foreground whitespace-nowrap">2024-01-17,Salário,5000.00,PIX,Conta Corrente,,Receita</p>
            </div>
            <div className="mt-3 space-y-2 text-sm">
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">date</strong>
                  <span className="text-muted-foreground"> (obrigatório): Formato YYYY-MM-DD (ex: 2024-01-15)</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">description</strong>
                  <span className="text-muted-foreground"> (obrigatório): Texto descritivo da transação</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">amount</strong>
                  <span className="text-muted-foreground"> (obrigatório): Valor em reais. Use negativo para despesas (ex: -150.50 ou 5000.00)</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">payment_method</strong>
                  <span className="text-muted-foreground"> (opcional): "PIX" ou "CARD" (padrão: PIX)</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">account</strong>
                  <span className="text-muted-foreground"> (obrigatório para PIX): Nome da conta (ex: "Conta Corrente")</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">card</strong>
                  <span className="text-muted-foreground"> (obrigatório para CARD): Nome do cartão (ex: "Cartão Nubank")</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <strong className="text-foreground">category</strong>
                  <span className="text-muted-foreground"> (opcional): Nome da categoria (ex: "Alimentação")</span>
                </div>
              </div>
            </div>
            
            {/* Important notes */}
            <div className="mt-4 p-3 rounded-lg bg-blue-500/10 border border-blue-500/20">
              <p className="text-sm font-medium text-foreground mb-2">📝 Notas importantes:</p>
              <ul className="text-sm text-muted-foreground space-y-1 list-disc list-inside">
                <li>O arquivo pode conter uma mistura de transações PIX e CARTÃO</li>
                <li>Para transações PIX, a coluna "account" deve conter o nome da conta</li>
                <li>Para transações CARTÃO, defina payment_method=CARD e especifique o cartão na coluna "card"</li>
                <li>Se payment_method não for especificado, assume-se PIX como padrão</li>
                <li>A conta selecionada abaixo será usada para associação do lote de importação</li>
              </ul>
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
                <Label>Conta (para associação do lote)</Label>
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
                <p className="text-xs text-muted-foreground">
                  Conta usada para associar o lote de importação. Transações individuais usam a conta/cartão do CSV.
                </p>
              </div>
              <div className="space-y-2">
                <Label>Arquivo</Label>
                <Input
                  type="file"
                  accept=".csv"
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
                  key={job.batchId}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
                      <FileText className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-foreground">{job.filename}</p>
                      <p className="text-sm text-muted-foreground">
                        Conta: {accounts.find((a) => a.id === job.accountId)?.name || job.accountId}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {job.totals.committedRows}/{job.totals.totalRows} linhas · {job.totals.errorRows} falhas
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2">
                      {statusIcon(job.status)}
                      <span className={`text-sm font-medium ${
                        job.status === "COMMITTED" ? "text-primary" :
                        job.status === "FAILED" ? "text-destructive" : "text-yellow-500"
                      }`}>
                        {statusLabel(job.status)}
                      </span>
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
