"use client"

import { useCallback, useEffect, useState, useMemo } from "react"
import Link from "next/link"
import { apiClient } from "@/lib/api-client"
import { formatCents, formatPercent, monthToday } from "@/lib/format"
import { useAppToast } from "@/contexts/toast-context"
import { getAlertTypeLabel, getGoalStatusLabel } from "@/lib/constants/labels"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Progress } from "@/components/ui/progress"
import { Checkbox } from "@/components/ui/checkbox"
import { WidgetConfig, WIDGET_KEYS, WidgetKey } from "@/lib/types/dashboard"
import { CardLimitsWidget } from "@/components/dashboard/CardLimitsWidget"
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  PiggyBank,
  Bell,
  Target,
  ArrowRight,
  AlertCircle,
  GripVertical,
  LayoutDashboard,
  RefreshCw,
  CreditCard,
} from "lucide-react"
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from "@dnd-kit/core"
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts"

interface DashboardData {
  incomeCents: number
  expenseCents: number
  netCents: number
  byCategory: Array<{
    categoryId: number
    categoryName: string
    categoryColor: string | null
    expenseCents: number
  }>
  budgetStatus: Array<{
    budgetId: number
    categoryId: number
    limitCents: number
    consumptionCents: number
    percent: number
    triggered80: boolean
    triggered100: boolean
  }>
  alerts: Array<{
    id: number
    type: string
    message: string
    triggeredAt: string
    isRead: boolean
  }>
  goalsSummary: Array<{
    goalId: number
    name: string
    percent: number
    status: string
    neededMonthlyCents: number
  }>
}

interface Account {
  id: number
  name: string
  balanceCents: number
}

interface Category {
  id: number
  name: string
  color: string | null
}

type DashboardCardType = "accounts" | "categories" | "budgets" | "alerts" | "goals" | "card_limits"

const WIDGET_KEY_TO_CARD_TYPE: Record<string, DashboardCardType> = {
  [WIDGET_KEYS.SUMMARY]: "accounts",
  [WIDGET_KEYS.BUDGETS]: "budgets",
  [WIDGET_KEYS.GOALS]: "goals",
  [WIDGET_KEYS.ALERTS]: "alerts",
  [WIDGET_KEYS.CARD_LIMITS]: "card_limits",
}

const CARD_TYPE_TO_WIDGET_KEY: Record<DashboardCardType, string> = {
  accounts: WIDGET_KEYS.SUMMARY,
  categories: WIDGET_KEYS.SUMMARY,
  budgets: WIDGET_KEYS.BUDGETS,
  alerts: WIDGET_KEYS.ALERTS,
  goals: WIDGET_KEYS.GOALS,
  card_limits: WIDGET_KEYS.CARD_LIMITS,
}

const WIDGET_LABELS: Record<string, string> = {
  [WIDGET_KEYS.SUMMARY]: "Resumo",
  [WIDGET_KEYS.BUDGETS]: "Orçamentos",
  [WIDGET_KEYS.GOALS]: "Metas",
  [WIDGET_KEYS.ALERTS]: "Alertas",
  [WIDGET_KEYS.CARD_LIMITS]: "Limites de Cartão",
}

const DEFAULT_CARD_ORDER: DashboardCardType[] = [
  "accounts",
  "categories",
  "budgets",
  "alerts",
  "goals",
  "card_limits",
]

interface SortableCardProps {
  id: string
  children: React.ReactNode
}

function SortableCard({ id, children }: SortableCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div ref={setNodeRef} style={style} className="relative">
      <div 
        className="absolute top-4 right-4 z-10 cursor-grab active:cursor-grabbing" 
        {...attributes} 
        {...listeners}
        role="button"
        aria-label="Drag handle"
        tabIndex={0}
      >
        <GripVertical className="h-5 w-5 text-muted-foreground hover:text-foreground transition-colors" />
      </div>
      {children}
    </div>
  )
}

export default function DashboardPage() {
  const [month, setMonth] = useState(monthToday())
  const [data, setData] = useState<DashboardData | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [isEditMode, setIsEditMode] = useState(false)
  const [cardOrder, setCardOrder] = useState<DashboardCardType[]>(DEFAULT_CARD_ORDER)
  const [widgetConfigs, setWidgetConfigs] = useState<WidgetConfig[]>([])
  const [widgetsLoading, setWidgetsLoading] = useState(true)
  const { addToast } = useAppToast()

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  const categoryMap = useMemo(() => {
    const map = new Map<number, Category>()
    categories.forEach(cat => map.set(cat.id, cat))
    return map
  }, [categories])

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const [dashboardResponse, accountsResponse, categoriesResponse] = await Promise.all([
        apiClient.get<DashboardData>(`/api/dashboard/monthly?month=${month}`),
        apiClient.get<Account[]>("/api/accounts"),
        apiClient.get<Category[]>("/api/categories"),
      ])
      setData(dashboardResponse)
      setAccounts(accountsResponse || [])
      setCategories(categoriesResponse || [])
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro ao carregar dashboard"
      setError(message)
      addToast("Erro ao carregar dashboard.", "error")
    } finally {
      setLoading(false)
    }
  }, [month, addToast])

  const loadWidgetConfigs = useCallback(async () => {
    setWidgetsLoading(true)
    try {
      const configs = await apiClient.get<WidgetConfig[]>("/api/dashboard/widgets")
      setWidgetConfigs(configs || [])
      
      // Build card order from widget configs
      const enabledWidgets = configs
        .filter(w => w.isEnabled)
        .sort((a, b) => a.displayOrder - b.displayOrder)
      
      const newOrder: DashboardCardType[] = []
      
      // Always include accounts and categories first (from SUMMARY widget)
      const summaryWidget = enabledWidgets.find(w => w.widgetKey === WIDGET_KEYS.SUMMARY)
      if (summaryWidget) {
        newOrder.push("accounts", "categories")
      }
      
      // Add other widgets
      enabledWidgets.forEach(widget => {
        if (widget.widgetKey !== WIDGET_KEYS.SUMMARY && WIDGET_KEY_TO_CARD_TYPE[widget.widgetKey]) {
          newOrder.push(WIDGET_KEY_TO_CARD_TYPE[widget.widgetKey])
        }
      })
      
      setCardOrder(newOrder)
    } catch (err) {
      // Fallback to localStorage or default
      const savedLayout = localStorage.getItem("dashboard-card-order")
      if (savedLayout) {
        try {
          const parsed = JSON.parse(savedLayout) as DashboardCardType[]
          if (Array.isArray(parsed)) {
            setCardOrder(parsed)
          }
        } catch {
          setCardOrder(DEFAULT_CARD_ORDER)
        }
      }
    } finally {
      setWidgetsLoading(false)
    }
  }, [])

  const saveWidgetConfigs = useCallback(async (configs: WidgetConfig[]) => {
    try {
      await apiClient.put("/api/dashboard/widgets", configs)
      addToast("Configuração salva com sucesso.", "success")
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro ao salvar configuração"
      addToast(message, "error")
    }
  }, [addToast])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  useEffect(() => {
    loadWidgetConfigs()
  }, [loadWidgetConfigs])

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event

    if (over && active.id !== over.id) {
      setCardOrder((items) => {
        const oldIndex = items.indexOf(active.id as DashboardCardType)
        const newIndex = items.indexOf(over.id as DashboardCardType)
        
        if (oldIndex < 0 || newIndex < 0) {
          return items
        }
        
        const newOrder = arrayMove(items, oldIndex, newIndex)
        
        // Save to localStorage as backup
        localStorage.setItem("dashboard-card-order", JSON.stringify(newOrder))
        
        // Build updated widget configs with new display order
        const updatedConfigs = [...widgetConfigs]
        const cardTypeToOrderMap = new Map<string, number>()
        
        newOrder.forEach((cardType, index) => {
          const widgetKey = CARD_TYPE_TO_WIDGET_KEY[cardType]
          if (widgetKey && !cardTypeToOrderMap.has(widgetKey)) {
            cardTypeToOrderMap.set(widgetKey, index)
          }
        })
        
        updatedConfigs.forEach(config => {
          const newDisplayOrder = cardTypeToOrderMap.get(config.widgetKey)
          if (newDisplayOrder !== undefined) {
            config.displayOrder = newDisplayOrder
          }
        })
        
        setWidgetConfigs(updatedConfigs)
        saveWidgetConfigs(updatedConfigs)
        
        return newOrder
      })
    }
  }

  const handleWidgetToggle = (widgetKey: string, enabled: boolean) => {
    const updatedConfigs = widgetConfigs.map(config => 
      config.widgetKey === widgetKey ? { ...config, isEnabled: enabled } : config
    )
    setWidgetConfigs(updatedConfigs)
    
    // Update card order to reflect enabled/disabled widgets
    const enabledWidgets = updatedConfigs
      .filter(w => w.isEnabled)
      .sort((a, b) => a.displayOrder - b.displayOrder)
    
    const newOrder: DashboardCardType[] = []
    const summaryWidget = enabledWidgets.find(w => w.widgetKey === WIDGET_KEYS.SUMMARY)
    if (summaryWidget) {
      newOrder.push("accounts", "categories")
    }
    
    enabledWidgets.forEach(widget => {
      if (widget.widgetKey !== WIDGET_KEYS.SUMMARY && WIDGET_KEY_TO_CARD_TYPE[widget.widgetKey]) {
        newOrder.push(WIDGET_KEY_TO_CARD_TYPE[widget.widgetKey])
      }
    })
    
    setCardOrder(newOrder)
    saveWidgetConfigs(updatedConfigs)
  }

  const resetLayout = async () => {
    await loadWidgetConfigs()
    addToast("Layout restaurado.", "success")
  }

  const renderAccountsCard = () => (
    <Card className="bg-card border-border">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg flex items-center gap-2">
          <Wallet className="h-5 w-5 text-primary" />
          Saldos por conta
        </CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link href="/contas" className="flex items-center gap-1">
            Ver todas <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {accounts.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nenhuma conta cadastrada.</p>
        ) : (
          <div className="space-y-3">
            {accounts.map((account) => (
              <div key={account.id} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                <span className="text-foreground">{account.name}</span>
                <span className={`font-medium ${account.balanceCents >= 0 ? 'text-primary' : 'text-destructive'}`}>
                  {formatCents(account.balanceCents)}
                </span>
              </div>
            ))}
            <div className="flex items-center justify-between py-2 pt-4 border-t-2 border-border">
              <span className="font-bold text-foreground">Total Geral</span>
              <span className={`font-bold text-lg ${
                accounts.reduce((sum, acc) => sum + acc.balanceCents, 0) >= 0 ? 'text-primary' : 'text-destructive'
              }`}>
                {formatCents(accounts.reduce((sum, acc) => sum + acc.balanceCents, 0))}
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )

  const renderCategoriesCard = () => {
    const chartData = data?.byCategory.map((item) => ({
      name: item.categoryName,
      value: item.expenseCents / 100, // Convert to currency units for better readability
      color: item.categoryColor || "#6b7280",
      valueFormatted: formatCents(item.expenseCents),
    })) || []

    return (
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="text-lg">Gastos por categoria</CardTitle>
        </CardHeader>
        <CardContent>
          {data && data.byCategory.length === 0 ? (
            <p className="text-muted-foreground text-sm">Nenhum gasto categorizado.</p>
          ) : (
            <div className="space-y-6">
              {/* Pie Chart */}
              <div className="w-full h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: number) => formatCents(value * 100)}
                      contentStyle={{
                        backgroundColor: "hsl(var(--card))",
                        border: "1px solid hsl(var(--border))",
                        borderRadius: "8px",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              {/* List */}
              <div className="space-y-3">
                {data?.byCategory.map((item) => (
                  <div key={item.categoryId} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-3 h-3 rounded-full flex-shrink-0"
                        style={{ backgroundColor: item.categoryColor || "#6b7280" }}
                      />
                      <span className="text-foreground">{item.categoryName}</span>
                    </div>
                    <span className="font-medium text-foreground">{formatCents(item.expenseCents)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    )
  }

  const renderBudgetsCard = () => (
    <Card className="bg-card border-border">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg flex items-center gap-2">
          <PiggyBank className="h-5 w-5 text-primary" />
          Status de orcamentos
        </CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link href="/orcamentos" className="flex items-center gap-1">
            Ver todos <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {data && data.budgetStatus.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nenhum orcamento configurado.</p>
        ) : (
          <div className="space-y-4">
            {data?.budgetStatus.map((budget) => (
              <div key={budget.budgetId} className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-foreground">
                    {categoryMap.get(budget.categoryId)?.name || "Sem categoria"}
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">
                      {formatCents(budget.consumptionCents)} / {formatCents(budget.limitCents)}
                    </span>
                    <span className={`text-sm font-medium ${
                      budget.triggered100 ? 'text-destructive' : 
                      budget.triggered80 ? 'text-yellow-500' : 'text-primary'
                    }`}>
                      {formatPercent(budget.percent)}
                    </span>
                  </div>
                </div>
                <Progress 
                  value={Math.min(budget.percent, 100)} 
                  className={`h-2 ${
                    budget.triggered100 ? '[&>div]:bg-destructive' : 
                    budget.triggered80 ? '[&>div]:bg-yellow-500' : ''
                  }`}
                />
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )

  const renderAlertsCard = () => (
    <Card className="bg-card border-border">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg flex items-center gap-2">
          <Bell className="h-5 w-5 text-primary" />
          Alertas do mes
        </CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link href="/alertas" className="flex items-center gap-1">
            Ver todos <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {data && data.alerts.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nenhum alerta disparado.</p>
        ) : (
          <div className="space-y-3">
            {data?.alerts.slice(0, 5).map((alert) => (
              <div key={alert.id} className={`flex items-start justify-between py-2 border-b border-border last:border-0 ${alert.isRead ? 'opacity-50' : ''}`}>
                <div>
                  <p className="font-medium text-foreground">{getAlertTypeLabel(alert.type)}</p>
                  <p className="text-sm text-muted-foreground">{alert.message}</p>
                </div>
                <span className="text-xs text-muted-foreground">
                  {new Date(alert.triggeredAt).toLocaleDateString('pt-BR')}
                </span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )

  const renderGoalsCard = () => (
    <Card className="bg-card border-border">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg flex items-center gap-2">
          <Target className="h-5 w-5 text-primary" />
          Metas
        </CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link href="/metas" className="flex items-center gap-1">
            Ver todas <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {data && data.goalsSummary.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nenhuma meta ativa.</p>
        ) : (
          <div className="space-y-4">
            {data?.goalsSummary.map((goal) => (
              <div key={goal.goalId} className="space-y-2">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium text-foreground">{goal.name}</p>
                    <p className="text-xs text-muted-foreground">{getGoalStatusLabel(goal.status)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium text-primary">{formatPercent(goal.percent)}</p>
                    <p className="text-xs text-muted-foreground">
                      Necessario {formatCents(goal.neededMonthlyCents)}/mes
                    </p>
                  </div>
                </div>
                <Progress value={Math.min(goal.percent, 100)} className="h-2" />
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )

  const renderCard = (cardType: DashboardCardType) => {
    const cardContent = (() => {
      switch (cardType) {
        case "accounts":
          return renderAccountsCard()
        case "categories":
          return renderCategoriesCard()
        case "budgets":
          return renderBudgetsCard()
        case "alerts":
          return renderAlertsCard()
        case "goals":
          return renderGoalsCard()
        case "card_limits":
          return <CardLimitsWidget />
        default:
          return null
      }
    })()

    if (isEditMode) {
      return (
        <SortableCard key={cardType} id={cardType}>
          {cardContent}
        </SortableCard>
      )
    }

    return <div key={cardType}>{cardContent}</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-muted-foreground">Resumo mensal e alertas.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <Label htmlFor="month" className="text-muted-foreground">Mes</Label>
            <Input
              id="month"
              type="month"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              className="w-40 bg-input border-border"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={loadDashboard}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Atualizar
          </Button>
          <Button
            variant={isEditMode ? "default" : "outline"}
            size="sm"
            onClick={() => setIsEditMode(!isEditMode)}
          >
            <LayoutDashboard className="h-4 w-4 mr-2" />
            {isEditMode ? "Concluir" : "Personalizar"}
          </Button>
        </div>
      </div>

      {isEditMode && (
        <Card className="bg-primary/5 border-primary/20">
          <CardContent className="py-4 space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-foreground">
                Arraste os cards pela alça para reorganizar o layout. As alterações são salvas automaticamente.
              </p>
              <Button variant="outline" size="sm" onClick={resetLayout}>
                Restaurar Padrão
              </Button>
            </div>
            
            <div className="pt-4 border-t border-border">
              <h3 className="text-sm font-medium mb-3">Widgets Visíveis</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {widgetConfigs.map(widget => (
                  <div key={widget.widgetKey} className="flex items-center space-x-2">
                    <Checkbox
                      id={widget.widgetKey}
                      checked={widget.isEnabled}
                      onCheckedChange={(checked) => handleWidgetToggle(widget.widgetKey, checked === true)}
                    />
                    <label
                      htmlFor={widget.widgetKey}
                      className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                    >
                      {WIDGET_LABELS[widget.widgetKey] || widget.widgetKey}
                    </label>
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {loading && <DashboardSkeleton />}
      {error && (
        <Card className="bg-destructive/10 border-destructive/20">
          <CardContent className="flex items-center gap-3 py-4">
            <AlertCircle className="h-5 w-5 text-destructive" />
            <p className="text-destructive">{error}</p>
          </CardContent>
        </Card>
      )}

      {data && !loading && (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Receitas</p>
                    <p className="text-2xl font-bold text-primary">{formatCents(data.incomeCents)}</p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10">
                    <TrendingUp className="h-6 w-6 text-primary" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Despesas</p>
                    <p className="text-2xl font-bold text-destructive">{formatCents(data.expenseCents)}</p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-destructive/10">
                    <TrendingDown className="h-6 w-6 text-destructive" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-card border-border">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Saldo</p>
                    <p className={`text-2xl font-bold ${data.netCents >= 0 ? 'text-primary' : 'text-destructive'}`}>
                      {formatCents(data.netCents)}
                    </p>
                  </div>
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-secondary">
                    <Wallet className="h-6 w-6 text-foreground" />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Dynamic Cards with Drag and Drop */}
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={cardOrder}
              strategy={verticalListSortingStrategy}
            >
              <div className="space-y-4">
                {cardOrder.map((cardType) => renderCard(cardType))}
              </div>
            </SortableContext>
          </DndContext>
        </>
      )}
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Card key={i} className="bg-card border-border">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div className="space-y-2">
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-8 w-24" />
                </div>
                <Skeleton className="h-12 w-12 rounded-lg" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
      <Card className="bg-card border-border">
        <CardHeader>
          <Skeleton className="h-6 w-48" />
        </CardHeader>
        <CardContent className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
