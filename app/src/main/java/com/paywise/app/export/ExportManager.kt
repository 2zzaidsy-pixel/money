package com.paywise.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.paywise.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun exportToPdf(
        expenses: List<Expense>,
        goals: List<FinancialGoal>,
        salaryInfo: SalaryInfo,
        healthScore: FinancialHealthScore,
        onResult: (Result<Uri>) -> Unit
    ) {
        try {
            val fileName = "PayWise_Report_${dateFormat.format(Date())}.pdf"
            val file = File(context.cacheDir, fileName)
            val writer = PdfWriter(FileOutputStream(file))
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)

            document.add(Paragraph("PayWise Financial Report").setFontSize(24).setBold())
            document.add(Paragraph("Generated: ${displayDateFormat.format(Date())}"))
            document.add(Paragraph(" "))

            document.add(Paragraph("Salary Overview").setFontSize(18).setBold())
            document.add(Paragraph("Salary: ${String.format("%.0f", salaryInfo.salary)}"))
            document.add(Paragraph("Spent: ${String.format("%.0f", salaryInfo.totalSpent)}"))
            document.add(Paragraph("Remaining: ${String.format("%.0f", salaryInfo.remaining)}"))
            document.add(Paragraph(" "))

            document.add(Paragraph("Financial Health Score: ${healthScore.score}").setFontSize(18).setBold())
            document.add(Paragraph("Level: ${healthScore.level.name}"))
            document.add(Paragraph(" "))

            if (expenses.isNotEmpty()) {
                document.add(Paragraph("Expenses").setFontSize(18).setBold())
                val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
                table.useAllAvailableWidth()
                table.addHeaderCell("Category")
                table.addHeaderCell("Amount")
                table.addHeaderCell("Date")
                expenses.take(50).forEach { expense ->
                    table.addCell(expense.category.displayName)
                    table.addCell(String.format("%.0f", expense.amount))
                    table.addCell(displayDateFormat.format(Date(expense.date)))
                }
                document.add(table)
                document.add(Paragraph(" "))
            }

            if (goals.isNotEmpty()) {
                document.add(Paragraph("Financial Goals").setFontSize(18).setBold())
                goals.forEach { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100) else 0.0
                    document.add(Paragraph("${goal.title}: ${String.format("%.0f", goal.currentAmount)} / ${String.format("%.0f", goal.targetAmount)} (${String.format("%.0f", progress)}%)"))
                }
            }

            document.close()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            onResult(Result.success(uri))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun exportToCsv(
        expenses: List<Expense>,
        onResult: (Result<Uri>) -> Unit
    ) {
        try {
            val fileName = "PayWise_Export_${dateFormat.format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val sb = StringBuilder()

            sb.appendLine("Category,Amount,Date,Note")
            expenses.forEach { expense ->
                sb.appendLine("${expense.category.displayName},${expense.amount},${displayDateFormat.format(Date(expense.date))},\"${expense.note.replace("\"", "\"\"")}\"")
            }

            file.writeText(sb.toString())
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            onResult(Result.success(uri))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun shareReport(expenses: List<Expense>, goals: List<FinancialGoal>, salaryInfo: SalaryInfo, healthScore: FinancialHealthScore) {
        exportToPdf(expenses, goals, salaryInfo, healthScore) { result ->
            result.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share PayWise Report"))
            }
        }
    }
}
