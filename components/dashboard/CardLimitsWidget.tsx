"use client"

import { useEffect, useState } from "react"
import { CreditCard } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { formatCents } from "@/lib/format"
import { useAppToast } from "@/contexts/toast-context"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Skeleton } from "@/components/ui/skeleton"
import { CardLimitSummary } from "@/lib/types/dashboard"

export function CardLimitsWidget() {
  const [cardLimits, setCardLimits] = useState<CardLimitSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const { addToast } = useAppToast()

  useEffect(() => {
    const fetchCardLimits = async () => {
      setLoading(true)
      setError("")
      try {
        const data = await apiClient.get<CardLimitSummary[]>("/api/cards/limit-summary")
        setCardLimits(data || [])
      } catch (err) {
        const message = err instanceof Error ? err.message : "Erro ao carregar limites de cartão"
        setError(message)
        addToast("Erro ao carregar limites de cartão.", "error")
      } finally {
        setLoading(false)
      }
    }

    fetchCardLimits()
  }, [addToast])

  const getProgressColor = (percentUsed: number) => {
    if (percentUsed > 80) return "[&>div]:bg-destructive"
    if (percentUsed > 60) return "[&>div]:bg-yellow-500"
    return ""
  }

  if (loading) {
    return (
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <CreditCard className="h-5 w-5 text-primary" />
            Limites de Cartão
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-2 w-full" />
              <Skeleton className="h-3 w-48" />
            </div>
          ))}
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="bg-card border-border">
      <CardHeader>
        <CardTitle className="text-lg flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-primary" />
          Limites de Cartão
        </CardTitle>
      </CardHeader>
      <CardContent>
        {error && (
          <p className="text-sm text-destructive mb-4">{error}</p>
        )}
        {cardLimits.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nenhum cartão cadastrado.</p>
        ) : (
          <div className="space-y-4">
            {cardLimits.map((card) => (
              <div key={card.cardId} className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-foreground">{card.cardName}</span>
                  <span className={`text-sm font-medium ${
                    card.percentUsed > 80 ? 'text-destructive' : 
                    card.percentUsed > 60 ? 'text-yellow-500' : 'text-primary'
                  }`}>
                    {Math.round(card.percentUsed)}%
                  </span>
                </div>
                <Progress 
                  value={Math.min(card.percentUsed, 100)} 
                  className={`h-2 ${getProgressColor(card.percentUsed)}`}
                />
                <p className="text-xs text-muted-foreground">
                  {formatCents(card.usedCents)} usado de {formatCents(card.limitTotal)}
                </p>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
