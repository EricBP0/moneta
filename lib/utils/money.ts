/**
 * Utility functions for handling money values
 * Users input in reais (e.g., 123,45), API expects cents (e.g., 12345)
 */

/**
 * Parses a money string to cents (integer)
 * Accepts formats like: "1.234,56", "1234.56", "1234,56", "1234"
 * @param value - The string value to parse
 * @returns The value in cents (integer)
 */
export function parseMoneyToCents(value: string | number): number {
  if (typeof value === 'number') {
    return Math.round(value * 100)
  }

  // Remove whitespace
  let cleanValue = value.trim()

  // If empty, return 0
  if (!cleanValue) {
    return 0
  }

  // Detect if comma is decimal separator (Brazilian format)
  // If there's a comma after a dot, or comma is the last separator, it's decimal
  const hasComma = cleanValue.includes(',')
  const hasDot = cleanValue.includes('.')

  if (hasComma && hasDot) {
    // Both present - the last one is the decimal separator
    const lastComma = cleanValue.lastIndexOf(',')
    const lastDot = cleanValue.lastIndexOf('.')
    
    if (lastComma > lastDot) {
      // Comma is decimal: remove dots (thousands), replace comma with dot
      cleanValue = cleanValue.replace(/\./g, '').replace(',', '.')
    } else {
      // Dot is decimal: remove commas (thousands)
      cleanValue = cleanValue.replace(/,/g, '')
    }
  } else if (hasComma) {
    // Only comma - assume it's decimal separator (Brazilian format)
    cleanValue = cleanValue.replace(',', '.')
  }
  // If only dot, assume it's already in correct format

  // Parse as float and convert to cents
  const floatValue = parseFloat(cleanValue)
  
  // Return 0 for invalid input to prevent form submission errors
  // Form-level validation should catch invalid inputs before submission
  if (isNaN(floatValue)) {
    return 0
  }

  return Math.round(floatValue * 100)
}

/**
 * Formats cents (integer) to money string in Brazilian Real format
 * @param cents - The value in cents
 * @param currency - The currency code (default: BRL)
 * @returns Formatted money string (e.g., "R$ 1.234,56")
 */
export function formatCentsToMoney(
  cents: number | null | undefined,
  currency: string = 'BRL'
): string {
  if (cents === null || cents === undefined) {
    return '—'
  }

  const value = cents / 100

  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: currency,
  }).format(value)
}

/**
 * Formats cents to a plain number string for input fields
 * @param cents - The value in cents
 * @returns Plain number string in Brazilian format (e.g., "1234,56")
 */
export function formatCentsToInput(cents: number | null | undefined): string {
  if (cents === null || cents === undefined || isNaN(cents)) {
    return ''
  }

  const value = cents / 100
  return value.toFixed(2).replace('.', ',')
}
