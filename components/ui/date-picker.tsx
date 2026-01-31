"use client"

import * as React from "react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import { Calendar as CalendarIcon } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

interface DatePickerProps {
  value?: Date
  onChange?: (date: Date | undefined) => void
  placeholder?: string
  disabled?: boolean
  className?: string
}

export function DatePicker({
  value,
  onChange,
  placeholder = "Selecione uma data",
  disabled = false,
  className,
}: DatePickerProps) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          variant={"outline"}
          className={cn(
            "w-full justify-start text-left font-normal",
            !value && "text-muted-foreground",
            className
          )}
          disabled={disabled}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {value ? format(value, "dd/MM/yyyy", { locale: ptBR }) : <span>{placeholder}</span>}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={value}
          onSelect={onChange}
          initialFocus
        />
      </PopoverContent>
    </Popover>
  )
}

interface DateRangePickerProps {
  from?: Date
  to?: Date
  onFromChange?: (date: Date | undefined) => void
  onToChange?: (date: Date | undefined) => void
  placeholder?: string
  disabled?: boolean
  className?: string
}

export function DateRangePicker({
  from,
  to,
  onFromChange,
  onToChange,
  placeholder = "Selecione o período",
  disabled = false,
  className,
}: DateRangePickerProps) {
  const [showCalendar, setShowCalendar] = React.useState<'from' | 'to' | null>(null)

  return (
    <div className={cn("flex gap-2", className)}>
      <Popover open={showCalendar === 'from'} onOpenChange={(open) => setShowCalendar(open ? 'from' : null)}>
        <PopoverTrigger asChild>
          <Button
            variant={"outline"}
            className={cn(
              "flex-1 justify-start text-left font-normal",
              !from && "text-muted-foreground"
            )}
            disabled={disabled}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {from ? format(from, "dd/MM/yyyy", { locale: ptBR }) : <span>Data início</span>}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={from}
            onSelect={(date) => {
              onFromChange?.(date)
              setShowCalendar(null)
            }}
            initialFocus
          />
        </PopoverContent>
      </Popover>

      <Popover open={showCalendar === 'to'} onOpenChange={(open) => setShowCalendar(open ? 'to' : null)}>
        <PopoverTrigger asChild>
          <Button
            variant={"outline"}
            className={cn(
              "flex-1 justify-start text-left font-normal",
              !to && "text-muted-foreground"
            )}
            disabled={disabled}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {to ? format(to, "dd/MM/yyyy", { locale: ptBR }) : <span>Data fim</span>}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={to}
            onSelect={(date) => {
              onToChange?.(date)
              setShowCalendar(null)
            }}
            initialFocus
            disabled={(date) => from ? date < from : false}
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}
