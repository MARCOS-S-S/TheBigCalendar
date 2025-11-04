package com.mss.thebigcalendar.data.service

import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.kernel.geom.Rectangle
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.JsonHoliday
import com.mss.thebigcalendar.ui.components.MoonPhase
import com.mss.thebigcalendar.ui.screens.PageOrientation
import com.mss.thebigcalendar.ui.screens.PrintOptions
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mss.thebigcalendar.ui.screens.PageSize as CustomPageSize

class PdfGenerationService {
    
    /**
     * Converte uma cor do Compose para uma cor do iText PDF
     */
    private fun convertComposeColorToITextColor(composeColor: androidx.compose.ui.graphics.Color): Color {
        return DeviceRgb(
            composeColor.red,
            composeColor.green,
            composeColor.blue
        )
    }
    
    fun generateCalendarPdf(
        printOptions: PrintOptions,
        activities: List<Activity>,
        holidays: List<Holiday>,
        jsonHolidays: List<JsonHoliday>,
        moonPhases: List<MoonPhase>
    ): File {
        
        // Configurar página
        val pageSize = when (printOptions.pageSize) {
            CustomPageSize.A4 -> if (printOptions.orientation == PageOrientation.LANDSCAPE) 
                PageSize.A4.rotate() else PageSize.A4
            CustomPageSize.A3 -> if (printOptions.orientation == PageOrientation.LANDSCAPE) 
                PageSize.A3.rotate() else PageSize.A3
        }
        
        // Criar arquivo no diretório de Downloads
        val downloadsDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "TheBigCalendar")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val fileName = "calendario_${printOptions.selectedMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"))}.pdf"
        val outputFile = File(downloadsDir, fileName)
        
        // Garantir que o arquivo seja sobrescrito se já existir
        if (outputFile.exists()) {
            outputFile.delete()
            android.util.Log.d("PdfGenerationService", "🗑️ Arquivo existente removido: ${outputFile.name}")
        }
        
        val writer = PdfWriter(outputFile)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, pageSize)
        
        // Configurar margens mínimas para ocupar todo o espaço da página
        document.setMargins(20f, 20f, 20f, 20f)
        
        android.util.Log.d("PdfGenerationService", "📄 Criando novo arquivo PDF: ${outputFile.absolutePath}")
        
        // Aplicar cor de fundo da página
        val pageBackgroundColor = convertComposeColorToITextColor(printOptions.pageBackgroundColor)
        
        // Criar evento de página para aplicar cor de fundo
        pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, 
            object : com.itextpdf.kernel.events.IEventHandler {
                override fun handleEvent(event: com.itextpdf.kernel.events.Event?) {
                    val docEvent = event as? com.itextpdf.kernel.events.PdfDocumentEvent
                    if (docEvent != null) {
                        val pdfDoc = docEvent.document
                        val page = docEvent.page
                        val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
                        
                        val pageRect = page.pageSize
                        canvas
                            .saveState()
                            .setFillColor(pageBackgroundColor)
                            .rectangle(pageRect.left.toDouble(),
                                pageRect.bottom.toDouble(), pageRect.width.toDouble(), pageRect.height.toDouble()
                            )
                            .fill()
                            .restoreState()
                    }
                }
            })
        
        try {
            // Configurar fontes
            val titleFont = PdfFontFactory.createFont()
            val headerFont = PdfFontFactory.createFont()
            val dayFont = PdfFontFactory.createFont()
            val contentFont = PdfFontFactory.createFont()
            
            // Título do calendário - mês e ano na mesma linha
            val monthName = printOptions.selectedMonth.format(
                DateTimeFormatter.ofPattern("MMMM", Locale("pt", "BR"))
            ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
            
            val yearNumber = printOptions.selectedMonth.format(
                DateTimeFormatter.ofPattern("yyyy", Locale("pt", "BR"))
            )
            
            // Criar tabela com 3 colunas para posicionar mês e ano na mesma linha
            val titleTable = Table(UnitValue.createPercentArray(3)).useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(5f)
            
            // Preparar células vazias
            val leftContent = StringBuilder()
            val centerContent = StringBuilder()
            val rightContent = StringBuilder()
            
            // Adicionar mês à posição correta
            when (printOptions.monthPosition) {
                com.mss.thebigcalendar.ui.screens.TitlePosition.LEFT -> leftContent.append(monthName)
                com.mss.thebigcalendar.ui.screens.TitlePosition.CENTER -> centerContent.append(monthName)
                com.mss.thebigcalendar.ui.screens.TitlePosition.RIGHT -> rightContent.append(monthName)
            }
            
            // Adicionar ano à posição correta (com espaço se for na mesma célula que o mês)
            when (printOptions.yearPosition) {
                com.mss.thebigcalendar.ui.screens.TitlePosition.LEFT -> {
                    if (leftContent.isNotEmpty()) leftContent.append(" ")
                    leftContent.append(yearNumber)
                }
                com.mss.thebigcalendar.ui.screens.TitlePosition.CENTER -> {
                    if (centerContent.isNotEmpty()) centerContent.append(" ")
                    centerContent.append(yearNumber)
                }
                com.mss.thebigcalendar.ui.screens.TitlePosition.RIGHT -> {
                    if (rightContent.isNotEmpty()) rightContent.append(" ")
                    rightContent.append(yearNumber)
                }
            }
            
            // Determinar tamanho da fonte para cada célula
            // Se mês e ano estão na mesma célula, usar o tamanho do mês como padrão
            val leftFontSize = when {
                leftContent.contains(monthName) && leftContent.contains(yearNumber) -> printOptions.monthFontSize.size
                leftContent.contains(monthName) -> printOptions.monthFontSize.size
                leftContent.contains(yearNumber) -> printOptions.yearFontSize.size
                else -> 24f
            }
            
            val centerFontSize = when {
                centerContent.contains(monthName) && centerContent.contains(yearNumber) -> printOptions.monthFontSize.size
                centerContent.contains(monthName) -> printOptions.monthFontSize.size
                centerContent.contains(yearNumber) -> printOptions.yearFontSize.size
                else -> 24f
            }
            
            val rightFontSize = when {
                rightContent.contains(monthName) && rightContent.contains(yearNumber) -> printOptions.monthFontSize.size
                rightContent.contains(monthName) -> printOptions.monthFontSize.size
                rightContent.contains(yearNumber) -> printOptions.yearFontSize.size
                else -> 24f
            }
            
            // Determinar cor do texto para cada célula
            val leftTextColor = when {
                leftContent.contains(monthName) && leftContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                leftContent.contains(monthName) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                leftContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.yearTextColor)
                else -> com.itextpdf.kernel.colors.ColorConstants.BLACK
            }
            
            val centerTextColor = when {
                centerContent.contains(monthName) && centerContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                centerContent.contains(monthName) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                centerContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.yearTextColor)
                else -> com.itextpdf.kernel.colors.ColorConstants.BLACK
            }
            
            val rightTextColor = when {
                rightContent.contains(monthName) && rightContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                rightContent.contains(monthName) -> convertComposeColorToITextColor(printOptions.monthTextColor)
                rightContent.contains(yearNumber) -> convertComposeColorToITextColor(printOptions.yearTextColor)
                else -> com.itextpdf.kernel.colors.ColorConstants.BLACK
            }
            
            // Criar células com conteúdo, tamanhos de fonte e cores apropriados
            val leftCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
            if (leftContent.isNotEmpty()) {
                leftCell.add(Paragraph(leftContent.toString())
                    .setFont(titleFont)
                    .setFontSize(leftFontSize)
                    .setFontColor(leftTextColor))
            }
            
            val centerCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
            if (centerContent.isNotEmpty()) {
                centerCell.add(Paragraph(centerContent.toString())
                    .setFont(titleFont)
                    .setFontSize(centerFontSize)
                    .setFontColor(centerTextColor))
            }
            
            val rightCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
            if (rightContent.isNotEmpty()) {
                rightCell.add(Paragraph(rightContent.toString())
                    .setFont(titleFont)
                    .setFontSize(rightFontSize)
                    .setFontColor(rightTextColor))
            }
            
            // Adicionar células à tabela
            titleTable.addCell(leftCell)
            titleTable.addCell(centerCell)
            titleTable.addCell(rightCell)
            
            document.add(titleTable)
            
            // Criar tabela do calendário
            val calendarTable = createCalendarTable(
                printOptions.selectedMonth,
                activities,
                holidays,
                jsonHolidays,
                moonPhases,
                printOptions,
                dayFont,
                contentFont,
                pdf,
                pageSize
            )
            
            document.add(calendarTable)
            
            // Adicionar legenda das fases da lua (se configurado)
            if (printOptions.includeMoonPhases && 
                printOptions.moonPhasePosition == com.mss.thebigcalendar.ui.screens.MoonPhasePosition.BELOW_CALENDAR) {
                val moonPhaseLegend = createMoonPhaseLegend(moonPhases, titleFont, pdf)
                document.add(moonPhaseLegend)
            }
            
        } finally {
            document.close()
        }
        
        return outputFile
    }
    
    private fun createCalendarTable(
        month: java.time.YearMonth,
        activities: List<Activity>,
        holidays: List<Holiday>,
        jsonHolidays: List<JsonHoliday>,
        moonPhases: List<MoonPhase>,
        printOptions: PrintOptions,
        dayFont: com.itextpdf.kernel.font.PdfFont,
        contentFont: com.itextpdf.kernel.font.PdfFont,
        pdfDocument: PdfDocument,
        pageSize: PageSize
    ): Table {
        
        // Criar tabela 7x6 (7 dias da semana, 6 semanas máximo)
        val table = Table(UnitValue.createPercentArray(7)).useAllAvailableWidth()
            .setMarginTop(2f)
        
        // Cabeçalho com dias da semana
        val weekDays = when (printOptions.weekDayAbbreviation) {
            com.mss.thebigcalendar.ui.screens.WeekDayAbbreviation.SHORT -> listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
            com.mss.thebigcalendar.ui.screens.WeekDayAbbreviation.FULL -> listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
        }
        weekDays.forEach { day ->
            val cell = Cell()
                .add(Paragraph(day)
                    .setFont(dayFont)
                    .setFontSize(printOptions.weekDayFontSize.size)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorder(Border.NO_BORDER)
                .setPadding(2f)
                .setMinHeight(15f)
            table.addCell(cell)
        }
        
        // Obter primeiro dia do mês e ajustar para domingo
        val firstDayOfMonth = month.atDay(1)
        val firstSunday = firstDayOfMonth.minusDays((firstDayOfMonth.dayOfWeek.value % 7).toLong())
        
        // Gerar células do calendário (6 semanas)
        for (week in 0..5) {
            for (dayOfWeek in 0..6) {
                val currentDate = firstSunday.plusDays(((week * 7) + dayOfWeek).toLong())
                val cell = createDayCell(
                    currentDate,
                    month,
                    activities,
                    holidays,
                    jsonHolidays,
                    moonPhases,
                    printOptions,
                    dayFont,
                    contentFont,
                    pdfDocument,
                    pageSize
                )
                table.addCell(cell)
            }
        }
        
        return table
    }
    
    private fun cmToPoints(cm: Float): Float {
        return cm * 28.35f
    }

    private fun createDayCell(
        date: LocalDate,
        month: java.time.YearMonth,
        activities: List<Activity>,
        holidays: List<Holiday>,
        jsonHolidays: List<JsonHoliday>,
        moonPhases: List<MoonPhase>,
        printOptions: PrintOptions,
        dayFont: com.itextpdf.kernel.font.PdfFont,
        contentFont: com.itextpdf.kernel.font.PdfFont,
        pdfDocument: PdfDocument,
        pageSize: PageSize
    ): Cell {
        
        val cell = Cell()
            .setPadding(1f)
            .setMinHeight(cmToPoints(printOptions.dayCellHeight))
        
        // Configurar bordas baseado na opção
        if (printOptions.showDayBorders) {
            cell.setBorder(SolidBorder(1f))
        } else {
            cell.setBorder(Border.NO_BORDER)
        }
        
        // Verificar se é do mês atual
        val isCurrentMonth = date.month == month.month
        
        if (!isCurrentMonth) {
            // Dias de outros meses
            // Verificar se deve mostrar fase da lua neste dia
            val moonPhaseForDay = if (printOptions.includeMoonPhases && 
                                     printOptions.moonPhasePosition == com.mss.thebigcalendar.ui.screens.MoonPhasePosition.OTHER_MONTH_DAYS) {
                moonPhases.find { it.date == date }
            } else null
            
            if (printOptions.hideOtherMonthDays) {
                // Se a opção estiver ativada, criar célula vazia (a menos que tenha fase da lua)
                if (moonPhaseForDay != null) {
                    cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    addMoonPhaseDrawing(cell, moonPhaseForDay, contentFont, pdfDocument)
                } else {
                    cell.setBackgroundColor(ColorConstants.WHITE)
                    cell.add(Paragraph("")
                        .setFont(dayFont)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER))
                }
            } else {
                // Dias de outros meses - mostrar número ou fase da lua
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                
                if (moonPhaseForDay != null) {
                    addMoonPhaseDrawing(cell, moonPhaseForDay, contentFont, pdfDocument)
                } else {
                    cell.add(Paragraph(date.dayOfMonth.toString())
                        .setFont(dayFont)
                        .setFontSize(printOptions.dayNumberFontSize.size * 0.7f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY))
                }
            }
        } else {
            // Dias do mês atual - aplicar cor de fundo selecionada
            val backgroundColor = convertComposeColorToITextColor(printOptions.backgroundColor)
            cell.setBackgroundColor(backgroundColor)
            
            // Determinar cor do número do dia
            val dayNumberColor = if (printOptions.colorDayNumbersByEvents) {
                getDayNumberColor(date, activities, holidays, jsonHolidays, printOptions)
            } else {
                com.itextpdf.kernel.colors.ColorConstants.BLACK
            }
            
            // Número do dia - usar tamanho e cor configurados pelo usuário
            val dayParagraph = Paragraph(date.dayOfMonth.toString())
                .setFont(dayFont)
                .setFontSize(printOptions.dayNumberFontSize.size)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontColor(dayNumberColor)
            
            cell.add(dayParagraph)
            
            // Conteúdo do dia
            val dayContent = getDayContent(date, activities, holidays, jsonHolidays, moonPhases, printOptions)
            
            dayContent.forEach { content ->
                val contentParagraph = Paragraph(content)
                    .setFont(contentFont)
                    .setFontSize(8f)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(2f)
                
                cell.add(contentParagraph)
            }
        }
        
        return cell
    }
    
    /**
     * Cria um desenho vetorial da fase da lua
     */
    private fun createMoonPhaseImage(
        moonPhase: MoonPhase,
        pdfDocument: PdfDocument,
        size: Float = 30f
    ): Image {
        // Criar form XObject para desenhar a lua
        val form = PdfFormXObject(Rectangle(size, size))
        val canvas = PdfCanvas(form, pdfDocument)
        
        val centerX = (size / 2f).toDouble()
        val centerY = (size / 2f).toDouble()
        val radius = (size / 2.2f).toDouble()
        
        canvas.saveState()
        
        // Desenhar círculo externo (borda da lua)
        canvas.setStrokeColor(ColorConstants.DARK_GRAY)
        canvas.setLineWidth(1.5f)
        canvas.circle(centerX, centerY, radius)
        canvas.stroke()
        
        // Desenhar preenchimento baseado na fase
        when (moonPhase.phase) {
            com.mss.thebigcalendar.ui.components.MoonPhaseType.NEW_MOON -> {
                // Lua nova - círculo preto completo
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.FULL_MOON -> {
                // Lua cheia - círculo branco completo
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.FIRST_QUARTER -> {
                // Quarto crescente - metade direita branca
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.rectangle(centerX, centerY - radius, radius, radius * 2.0)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.LAST_QUARTER -> {
                // Quarto minguante - metade esquerda branca
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.rectangle(centerX - radius, centerY - radius, radius, radius * 2.0)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_CRESCENT -> {
                // Crescente - pequena fatia à direita
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.moveTo(centerX, centerY - radius)
                canvas.lineTo(centerX + (radius * 0.5), centerY)
                canvas.lineTo(centerX, centerY + radius)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_GIBBOUS -> {
                // Gibosa crescente - maior parte branca à direita
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.rectangle(centerX - radius, centerY - radius, radius * 0.5, radius * 2.0)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_CRESCENT -> {
                // Minguante - pequena fatia à esquerda
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.moveTo(centerX, centerY - radius)
                canvas.lineTo(centerX - (radius * 0.5), centerY)
                canvas.lineTo(centerX, centerY + radius)
                canvas.fill()
            }
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_GIBBOUS -> {
                // Gibosa minguante - maior parte branca à esquerda
                canvas.setFillColor(ColorConstants.WHITE)
                canvas.circle(centerX, centerY, radius)
                canvas.fill()
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.rectangle(centerX + (radius * 0.5), centerY - radius, radius * 0.5, radius * 2.0)
                canvas.fill()
            }
        }
        
        // Redesenhar borda por cima
        canvas.setStrokeColor(ColorConstants.DARK_GRAY)
        canvas.setLineWidth(1.5f)
        canvas.circle(centerX, centerY, radius)
        canvas.stroke()
        
        canvas.restoreState()
        
        return Image(form)
    }
    
    /**
     * Adiciona o desenho da fase da lua em uma célula
     */
    private fun addMoonPhaseDrawing(
        cell: Cell,
        moonPhase: MoonPhase,
        font: com.itextpdf.kernel.font.PdfFont,
        pdfDocument: PdfDocument
    ) {
        // Criar e adicionar desenho da lua
        val moonImage = createMoonPhaseImage(moonPhase, pdfDocument, 35f)
        cell.add(Paragraph().add(moonImage)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(8f))
        
        // Adicionar nome da fase (opcional, pequeno)
        val phaseName = when (moonPhase.phase) {
            com.mss.thebigcalendar.ui.components.MoonPhaseType.NEW_MOON -> "Nova"
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_CRESCENT -> "Crescente"
            com.mss.thebigcalendar.ui.components.MoonPhaseType.FIRST_QUARTER -> "Qto. Cresc."
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_GIBBOUS -> "Gib. Cresc."
            com.mss.thebigcalendar.ui.components.MoonPhaseType.FULL_MOON -> "Cheia"
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_GIBBOUS -> "Gib. Ming."
            com.mss.thebigcalendar.ui.components.MoonPhaseType.LAST_QUARTER -> "Qto. Ming."
            com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_CRESCENT -> "Minguante"
        }
        
        cell.add(Paragraph(phaseName)
            .setFont(font)
            .setFontSize(7f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(2f))
    }
    
    /**
     * Cria uma legenda com todas as fases da lua do mês
     */
    private fun createMoonPhaseLegend(
        moonPhases: List<MoonPhase>,
        font: com.itextpdf.kernel.font.PdfFont,
        pdfDocument: PdfDocument
    ): Table {
        // Criar tabela para a legenda (todas as fases na horizontal, mas 50% da largura)
        val numColumns = moonPhases.size.coerceAtLeast(1)
        val legendTable = Table(UnitValue.createPercentArray(numColumns))
            .setWidth(UnitValue.createPercentValue(50f))  // 50% da largura
            .setBorder(Border.NO_BORDER)
            .setMarginTop(10f)
        
        moonPhases.forEach { moonPhase ->
            val cell = Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4f)
            
            // Desenho vetorial da fase (menor para caber na legenda compacta)
            val moonImage = createMoonPhaseImage(moonPhase, pdfDocument, 20f)
            cell.add(Paragraph().add(moonImage)
                .setTextAlignment(TextAlignment.CENTER))
            
            // Nome da fase (abreviado)
            val phaseName = when (moonPhase.phase) {
                com.mss.thebigcalendar.ui.components.MoonPhaseType.NEW_MOON -> "Nova"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_CRESCENT -> "Cresc."
                com.mss.thebigcalendar.ui.components.MoonPhaseType.FIRST_QUARTER -> "Qto.C"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.WAXING_GIBBOUS -> "Gib.C"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.FULL_MOON -> "Cheia"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_GIBBOUS -> "Gib.M"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.LAST_QUARTER -> "Qto.M"
                com.mss.thebigcalendar.ui.components.MoonPhaseType.WANING_CRESCENT -> "Ming."
            }
            
            cell.add(Paragraph(phaseName)
                .setFont(font)
                .setFontSize(7f)
                .setTextAlignment(TextAlignment.CENTER))
            
            // Data
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
            cell.add(Paragraph(moonPhase.date.format(dateFormatter))
                .setFont(font)
                .setFontSize(7f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY))
            
            legendTable.addCell(cell)
        }
        
        // Aplicar estilo à tabela (50% da largura, horizontal)
        legendTable.setBorder(SolidBorder(1f))
        legendTable.setBackgroundColor(DeviceRgb(0.9f, 0.9f, 0.9f))
        
        return legendTable
    }
    
    /**
     * Determina a cor do número do dia baseado nos eventos daquele dia
     * Prioridade: Feriados > Dias de Santos > Aniversários > Eventos > Tarefas > Notas
     */
    private fun getDayNumberColor(
        date: LocalDate,
        activities: List<Activity>,
        holidays: List<Holiday>,
        jsonHolidays: List<JsonHoliday>,
        printOptions: PrintOptions
    ): Color {
        
        // Verificar feriados (maior prioridade)
        if (printOptions.includeHolidays) {
            val hasHoliday = holidays.any { LocalDate.parse(it.date) == date } ||
                           jsonHolidays.any { LocalDate.parse(it.date) == date }
            if (hasHoliday) {
                return convertComposeColorToITextColor(printOptions.holidayColor)
            }
        }
        
        // Verificar dias de santos (segunda prioridade)
        if (printOptions.includeSaintDays) {
            // TODO: Adicionar lógica quando dias de santos estiverem disponíveis
            // Por enquanto, vamos usar uma verificação placeholder
        }
        
        // Verificar aniversários
        if (printOptions.includeBirthdays) {
            val hasBirthday = activities.any { 
                LocalDate.parse(it.date) == date && 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY
            }
            if (hasBirthday) {
                return convertComposeColorToITextColor(printOptions.birthdayColor)
            }
        }
        
        // Verificar eventos
        if (printOptions.includeEvents) {
            val hasEvent = activities.any { 
                LocalDate.parse(it.date) == date && 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.EVENT
            }
            if (hasEvent) {
                return convertComposeColorToITextColor(printOptions.eventColor)
            }
        }
        
        // Verificar tarefas
        if (printOptions.includeTasks) {
            val hasTask = activities.any { 
                LocalDate.parse(it.date) == date && 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.TASK
            }
            if (hasTask) {
                return convertComposeColorToITextColor(printOptions.taskColor)
            }
        }
        
        // Verificar notas
        if (printOptions.includeNotes) {
            val hasNote = activities.any { 
                LocalDate.parse(it.date) == date && 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.NOTE
            }
            if (hasNote) {
                return convertComposeColorToITextColor(printOptions.noteColor)
            }
        }
        
        // Se não há eventos, retornar preto
        return com.itextpdf.kernel.colors.ColorConstants.BLACK
    }
    
    private fun getDayContent(
        date: LocalDate,
        activities: List<Activity>,
        holidays: List<Holiday>,
        jsonHolidays: List<JsonHoliday>,
        moonPhases: List<MoonPhase>,
        printOptions: PrintOptions
    ): List<String> {
        
        val dayContent = mutableListOf<String>()
        
        // Feriados nacionais
        if (printOptions.includeHolidays) {
            holidays.filter { holiday ->
                LocalDate.parse(holiday.date) == date
            }.forEach { holiday ->
                dayContent.add("🎉 ${holiday.name}")
            }
            
            jsonHolidays.filter { jsonHoliday ->
                LocalDate.parse(jsonHoliday.date) == date
            }.forEach { jsonHoliday ->
                dayContent.add("📅 ${jsonHoliday.name}")
            }
        }
        
        // Dias de Santos
        if (printOptions.includeSaintDays) {
            // TODO: Implementar quando disponível
        }
        
        // Tarefas
        if (printOptions.includeTasks) {
            activities.filter { activity ->
                LocalDate.parse(activity.date) == date &&
                activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.TASK
            }.forEach { activity ->
                val icon = if (activity.isCompleted == true) "✅" else "📝"
                dayContent.add("$icon ${activity.title}")
            }
        }
        
        // Eventos
        if (printOptions.includeEvents) {
            activities.filter { activity ->
                LocalDate.parse(activity.date) == date &&
                activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.EVENT
            }.forEach { activity ->
                dayContent.add("🎪 ${activity.title}")
            }
        }
        
        // Aniversários
        if (printOptions.includeBirthdays) {
            activities.filter { activity ->
                LocalDate.parse(activity.date) == date &&
                activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY
            }.forEach { activity ->
                dayContent.add("🎂 ${activity.title}")
            }
        }
        
        // Notas
        if (printOptions.includeNotes) {
            activities.filter { activity ->
                LocalDate.parse(activity.date) == date &&
                activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.NOTE
            }.forEach { activity ->
                dayContent.add("📝 ${activity.title}")
            }
        }
        
        // Tarefas completadas (separado das tarefas normais)
        if (printOptions.includeCompletedTasks) {
            activities.filter { activity ->
                LocalDate.parse(activity.date) == date &&
                activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.TASK &&
                activity.isCompleted == true
            }.forEach { activity ->
                dayContent.add("✅ ${activity.title}")
            }
        }
        
        // Fases da Lua - não adicionar aqui se estiverem sendo mostradas em outro lugar
        // (nos dias de outros meses ou na legenda abaixo)
        // Comentado para evitar duplicação
        // if (printOptions.includeMoonPhases) {
        //     moonPhases.filter { moonPhase ->
        //         moonPhase.date == date
        //     }.forEach { moonPhase ->
        //         dayContent.add("🌙 ${moonPhase.phase}")
        //     }
        // }
        
        // Limitar a 4 itens por dia para não sobrecarregar
        return dayContent.take(4)
    }
    
}