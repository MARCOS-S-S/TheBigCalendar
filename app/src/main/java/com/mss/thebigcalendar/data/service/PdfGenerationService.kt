package com.mss.thebigcalendar.data.service

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.JsonHoliday
import com.mss.thebigcalendar.ui.components.MoonPhase
import com.mss.thebigcalendar.ui.screens.PageOrientation
import com.mss.thebigcalendar.ui.screens.PageSize as CustomPageSize
import com.mss.thebigcalendar.ui.screens.PrintOptions
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class PdfGenerationService {
    
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
        
        val outputFile = File.createTempFile("calendar_${printOptions.selectedMonth}", ".pdf")
        val writer = PdfWriter(outputFile)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, pageSize)
        
        try {
            // Configurar fontes
            val titleFont = PdfFontFactory.createFont()
            val headerFont = PdfFontFactory.createFont()
            val dayFont = PdfFontFactory.createFont()
            val contentFont = PdfFontFactory.createFont()
            
            // Título do calendário
            val monthYear = printOptions.selectedMonth.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
            )
            val title = Paragraph(monthYear)
                .setFont(titleFont)
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            
            document.add(title)
            
            // Criar tabela do calendário
            val calendarTable = createCalendarTable(
                printOptions.selectedMonth,
                activities,
                holidays,
                jsonHolidays,
                moonPhases,
                printOptions,
                dayFont,
                contentFont
            )
            
            document.add(calendarTable)
            
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
        contentFont: com.itextpdf.kernel.font.PdfFont
    ): Table {
        
        // Criar tabela 7x6 (7 dias da semana, 6 semanas máximo)
        val table = Table(UnitValue.createPercentArray(7)).useAllAvailableWidth()
        
        // Cabeçalho com dias da semana
        val weekDays = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
        weekDays.forEach { day ->
            val cell = Cell()
                .add(Paragraph(day)
                    .setFont(dayFont)
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorder(Border.NO_BORDER)
                .setPadding(8f)
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
                    contentFont
                )
                table.addCell(cell)
            }
        }
        
        return table
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
        contentFont: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        
        val cell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setMinHeight(80f)
        
        // Verificar se é do mês atual
        val isCurrentMonth = date.month == month.month
        
        if (!isCurrentMonth) {
            // Dias de outros meses - mais claros
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
            cell.add(Paragraph(date.dayOfMonth.toString())
                .setFont(dayFont)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY))
        } else {
            // Dias do mês atual
            val isToday = date == LocalDate.now()
            
            if (isToday) {
                cell.setBackgroundColor(ColorConstants.YELLOW)
            }
            
            // Número do dia
            val dayParagraph = Paragraph(date.dayOfMonth.toString())
                .setFont(dayFont)
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
            
            if (isToday) {
                dayParagraph.setFontColor(ColorConstants.RED)
            }
            
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
        
        // Fases da Lua
        if (printOptions.includeMoonPhases) {
            moonPhases.filter { moonPhase ->
                moonPhase.date == date
            }.forEach { moonPhase ->
                dayContent.add("🌙 ${moonPhase.phase}")
            }
        }
        
        // Limitar a 4 itens por dia para não sobrecarregar
        return dayContent.take(4)
    }
}