package com.budgetflow.app

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class Transaction(
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: String
)

class MainActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("budgetflow", MODE_PRIVATE)
    }

    private val transactions = mutableListOf<Transaction>()

    private var monthlyIncome = 0L
    private var savingsGoal = 0L

    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var tvSavings: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var container: LinearLayout

    private val money =
        NumberFormat.getNumberInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpenses = findViewById(R.id.tvExpenses)
        tvSavings = findViewById(R.id.tvSavings)
        tvAdvice = findViewById(R.id.tvAdvice)
        container = findViewById(R.id.transactionsContainer)

        loadData()
        refresh()

        findViewById<Button>(R.id.btnIncome)
            .setOnClickListener {
                showAddIncome()
            }

        findViewById<Button>(R.id.btnExpense)
            .setOnClickListener {
                showAddExpense()
            }

        findViewById<Button>(R.id.btnGoal)
            .setOnClickListener {
                showGoalDialog()
            }

        findViewById<Button>(R.id.btnReset)
            .setOnClickListener {
                confirmReset()
            }
    }

    private fun fmt(value: Long): String {
        return "${money.format(value).replace(',', ' ')} FCFA"
    }

    private fun showAddIncome() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 0)
        }

        val amount = EditText(this).apply {
            hint = "Montant en FCFA"
            inputType = 2
        }

        val note = EditText(this).apply {
            hint = "Source (salaire, activité...)"
        }

        layout.addView(amount)
        layout.addView(note)

        AlertDialog.Builder(this)
            .setTitle("Ajouter un revenu")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->

                val value =
                    amount.text.toString().toLongOrNull()

                if (value != null && value > 0) {

                    monthlyIncome += value

                    transactions.add(
                        Transaction(
                            "income",
                            value,
                            "Revenu",
                            note.text.toString()
                                .ifBlank { "Revenu" },
                            today()
                        )
                    )

                    saveData()
                    refresh()

                } else {
                    toast("Montant invalide")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAddExpense() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 0)
        }

        val amount = EditText(this).apply {
            hint = "Montant en FCFA"
            inputType = 2
        }

        val category = Spinner(this)

        val categories = arrayOf(
            "Alimentation",
            "Transport",
            "Logement",
            "Internet",
            "Santé",
            "Loisirs",
            "Shopping",
            "Famille",
            "Autre"
        )

        category.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )

        val note = EditText(this).apply {
            hint = "Note (facultatif)"
        }

        layout.addView(amount)
        layout.addView(category)
        layout.addView(note)

        AlertDialog.Builder(this)
            .setTitle("Ajouter une dépense")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->

                val value =
                    amount.text.toString().toLongOrNull()

                if (value != null && value > 0) {

                    transactions.add(
                        Transaction(
                            "expense",
                            value,
                            category.selectedItem.toString(),
                            note.text.toString(),
                            today()
                        )
                    )

                    saveData()
                    refresh()

                } else {
                    toast("Montant invalide")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showGoalDialog() {

        val input = EditText(this).apply {
            hint = "Ex. 300000"
            inputType = 2
            setPadding(40, 20, 40, 10)
        }

        AlertDialog.Builder(this)
            .setTitle("Objectif d'épargne")
            .setMessage(
                "Combien veux-tu économiser ce mois-ci ?"
            )
            .setView(input)
            .setPositiveButton("Enregistrer") { _, _ ->

                val value =
                    input.text.toString().toLongOrNull()

                if (value != null && value >= 0) {

                    savingsGoal = value

                    saveData()
                    refresh()

                } else {
                    toast("Objectif invalide")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmReset() {

        AlertDialog.Builder(this)
            .setTitle("Réinitialiser ?")
            .setMessage(
                "Toutes les transactions et l'objectif seront supprimés."
            )
            .setPositiveButton("Réinitialiser") { _, _ ->

                monthlyIncome = 0
                savingsGoal = 0

                transactions.clear()

                saveData()
                refresh()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun refresh() {

        val expenses =
            transactions
                .filter { it.type == "expense" }
                .sumOf { it.amount }

        val balance =
            monthlyIncome - expenses

        val saved =
            max(0L, balance)

        tvIncome.text =
            "Revenus\n${fmt(monthlyIncome)}"

        tvExpenses.text =
            "Dépenses\n${fmt(expenses)}"

        tvBalance.text =
            fmt(balance)

        tvSavings.text =
            "Épargne prévue : ${fmt(savingsGoal)}"

        val advice = when {

            monthlyIncome == 0L ->
                "Ajoute ton revenu pour commencer."

            balance < 0 ->
                "⚠️ Tu as dépassé tes revenus de ${fmt(-balance)}."

            savingsGoal > 0 && saved >= savingsGoal ->
                "🎯 Objectif d'épargne atteint !"

            savingsGoal > 0 -> {

                val missing =
                    savingsGoal - saved

                "💡 Il te manque ${fmt(missing)} pour atteindre ton objectif."
            }

            else -> {

                val rate =
                    saved * 100.0 / monthlyIncome

                "📊 Il te reste ${"%.1f".format(Locale.US, rate)}% de ton revenu."
            }
        }

        tvAdvice.text = advice

        container.removeAllViews()

        transactions
            .asReversed()
            .take(20)
            .forEach { tx ->

                val row =
                    TextView(this).apply {

                        text =
                            if (tx.type == "expense") {

                                "− ${fmt(tx.amount)}  •  ${tx.category}" +
                                if (tx.note.isNotBlank())
                                    " — ${tx.note}"
                                else "" +
                                "\n${tx.date}"

                            } else {

                                "+ ${fmt(tx.amount)}  •  ${tx.note}" +
                                "\n${tx.date}"
                            }

                        textSize = 15f

                        setPadding(
                            16,
                            14,
                            16,
                            14
                        )

                        setTextColor(
                            if (tx.type == "expense")
                                0xFFC62828.toInt()
                            else
                                0xFF2E7D32.toInt()
                        )
                    }

                container.addView(row)
            }
    }

    private fun today(): String {

        return SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())
    }

    private fun saveData() {

        val data =
            transactions.joinToString("|||") {

                listOf(
                    it.type,
                    it.amount,
                    it.category,
                    it.note.replace("|", ""),
                    it.date
                ).joinToString("~~~")
            }

        prefs.edit()
            .putLong("income", monthlyIncome)
            .putLong("goal", savingsGoal)
            .putString("transactions", data)
            .apply()
    }

    private fun loadData() {

        monthlyIncome =
            prefs.getLong("income", 0)

        savingsGoal =
            prefs.getLong("goal", 0)

        val data =
            prefs.getString(
                "transactions",
                ""
            ) ?: ""

        if (data.isNotBlank()) {

            data.split("|||")
                .forEach { item ->

                    val p =
                        item.split("~~~")

                    if (p.size == 5) {

                        transactions.add(
                            Transaction(
                                p[0],
                                p[1].toLongOrNull() ?: 0,
                                p[2],
                                p[3],
                                p[4]
                            )
                        )
                    }
                }
        }
    }

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
