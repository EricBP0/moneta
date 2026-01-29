# UI Components

## Current Status

This project uses UI components based on the shadcn/ui design system. Currently, only the `toaster` component has been implemented.

## Missing Components

The following UI components are imported throughout the application but not yet implemented:

- `Button`
- `Input`
- `Label`
- `Card` (CardContent, CardHeader, CardTitle, CardDescription, CardFooter)
- `Select` (SelectContent, SelectItem, SelectTrigger, SelectValue)
- `Dialog` (DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription)
- `Table` (TableHeader, TableBody, TableRow, TableCell, TableHead)
- `Skeleton`
- `Progress`
- `Avatar` (AvatarFallback, AvatarImage)
- `DropdownMenu` (DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger)
- `Checkbox`
- `Badge`
- `Separator`
- `Tabs` (TabsList, TabsTrigger, TabsContent)
- `Form` (FormField, FormItem, FormLabel, FormControl, FormMessage)

## Setup Instructions

To add the missing UI components, you can use the shadcn/ui CLI:

```bash
# Install shadcn/ui CLI if not already installed
npx shadcn@latest init

# Add individual components as needed
npx shadcn@latest add button
npx shadcn@latest add input
npx shadcn@latest add label
npx shadcn@latest add card
npx shadcn@latest add select
npx shadcn@latest add dialog
npx shadcn@latest add table
npx shadcn@latest add skeleton
npx shadcn@latest add progress
npx shadcn@latest add avatar
npx shadcn@latest add dropdown-menu
npx shadcn@latest add checkbox
npx shadcn@latest add badge
npx shadcn@latest add separator
npx shadcn@latest add tabs
npx shadcn@latest add form
```

Or add all at once:
```bash
npx shadcn@latest add button input label card select dialog table skeleton progress avatar dropdown-menu checkbox badge separator tabs form
```

## Component Library

The components are based on:
- **Radix UI**: Unstyled, accessible components (already installed in package.json)
- **Tailwind CSS**: Utility-first CSS framework (configured in tailwind.config.js)
- **shadcn/ui**: Pre-built components that combine Radix + Tailwind

## References

- [shadcn/ui Documentation](https://ui.shadcn.com/)
- [Radix UI Documentation](https://www.radix-ui.com/)
- [Tailwind CSS Documentation](https://tailwindcss.com/)

## Note

Until the components are added, the application will have import errors. This is expected and documented here for transparency.
