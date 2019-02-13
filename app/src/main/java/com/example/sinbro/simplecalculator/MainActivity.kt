package com.example.sinbro.simplecalculator

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var textInput: TextView
    private var state: StateType = StateType.Empty
    private var emphasisBalance = 0

    enum class StateType {
        Digit,
        Operator,
        Dot,
        DigitAfterDot,
        OpenEmphasis,
        CloseEmphasis,
        Error,
        Empty

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textInput = findViewById(R.id.textInput)
        //restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt("state", when(state) {
            StateType.Digit -> 0
            StateType.Operator -> 1
            StateType.Dot -> 2
            StateType.DigitAfterDot -> 3
            StateType.OpenEmphasis -> 4
            StateType.CloseEmphasis -> 5
            StateType.Error -> 6
            StateType.Empty -> 7
        })
        outState?.putInt("emphasisBalance", emphasisBalance)
        outState?.putCharSequence("text", textInput.text)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreState(savedInstanceState)
    }

    private fun restoreState(savedState: Bundle?) {
        if (savedState != null) {
            textInput.text = savedState.getCharSequence("text")
            emphasisBalance = savedState.getInt("emphasisBalance")
            state = when(savedState.getInt("state")) {
                0 -> StateType.Digit
                1 -> StateType.Operator
                2 -> StateType.Dot
                3 -> StateType.DigitAfterDot
                4 -> StateType.OpenEmphasis
                5 -> StateType.CloseEmphasis
                6 -> StateType.Error
                7 -> StateType.Empty
                else -> StateType.Error
            }
        }
    }

    fun onDigit(view: View) {
        when (state) {
            StateType.Error -> {
                onClear(view)
                onDigit(view)
                return
            }

            StateType.CloseEmphasis -> {
                textInput.append("*")
                state = StateType.Operator
                onDigit(view)
                return
            }

            else -> textInput.append((view as Button).text)
        }

        if (state == StateType.Dot) {
            state = StateType.DigitAfterDot
        } else if (state != StateType.DigitAfterDot) {
            state = StateType.Digit
        }
    }

    fun onDot(view: View) {
        when (state) {
            StateType.Digit -> textInput.append(".")

            StateType.Empty, StateType.OpenEmphasis, StateType.Operator -> textInput.append("0.")

            StateType.CloseEmphasis -> textInput.append("*0.")

            StateType.Error -> textInput.text = "0."

            else -> return
        }
        state = StateType.Dot
    }

    @SuppressLint("SetTextI18n")
    fun onOperator(view: View) { // !!!!!!!!!!!!
        when (state) {
            StateType.Digit, StateType.DigitAfterDot, StateType.CloseEmphasis -> {
                textInput.append((view as Button).text)
            }

            StateType.Operator -> {
                onDelete(view)
                onOperator(view)
                return
            }

            StateType.Dot -> {
                deleteLastChar()
                state = StateType.Digit
                onOperator(view)
                return
            }

            StateType.Error -> {
                onClear(view)
                onOperator(view)
                return
            }

            StateType.Empty, StateType.OpenEmphasis -> {
                if ((view as Button).text == "-") {
                    textInput.append("-")
                } else {
                    return
                }
            }
        }

        state = StateType.Operator
    }

    fun onLEmphasis(view: View) {
        when (state) {
            StateType.Operator, StateType.Empty, StateType.OpenEmphasis -> {
                textInput.append("(")
            }

            StateType.Digit, StateType.DigitAfterDot, StateType.CloseEmphasis -> {
                textInput.append("*")
                state = StateType.Operator
                onLEmphasis(view)
                return
            }

            StateType.Dot -> {
                deleteLastChar()
                state = StateType.Digit
                onLEmphasis(view)
                return
            }

            StateType.Error -> {
                onClear(view)
                onLEmphasis(view)
                return
            }
        }

        state = StateType.OpenEmphasis
        ++emphasisBalance
    }

    fun onREmphasis(view: View) {
        if (emphasisBalance > 0) {
            when (state) {
                StateType.CloseEmphasis, StateType.Digit, StateType.DigitAfterDot -> textInput.append(")")

                StateType.Operator, StateType.Dot -> {
                    onDelete(view)
                    onREmphasis(view)
                    return
                }

                StateType.OpenEmphasis -> {
                    onDelete(view)
                    return
                }

                StateType.Error -> onClear(view)

                StateType.Empty -> return
            }

            state = StateType.CloseEmphasis
            --emphasisBalance
        }
    }

    fun onDelete(view: View) {
        if (state == StateType.Error) {
            onClear(view)
            return
        }

        if (state == StateType.CloseEmphasis) {
            ++emphasisBalance
        } else if (state == StateType.OpenEmphasis) {
            --emphasisBalance
        }

        if (state != StateType.Empty) {
            deleteLastChar()
            if (!textInput.text.isEmpty()) {
                when (textInput.text[textInput.text.length - 1]) {
                    '(' -> state = StateType.OpenEmphasis

                    ')' -> state = StateType.CloseEmphasis

                    '+', '-', '*', '/' -> state = StateType.Operator

                    '.' -> state = StateType.Dot

                    in '0'..'9' -> {
                        var i = textInput.text.length - 1
                        while (i > 0 && (textInput.text[i] in '0'..'9')) {
                            --i
                        }
                        state = if (textInput.text[i] == '.') StateType.DigitAfterDot else StateType.Digit
                    }
                }
            } else {
                state = StateType.Empty
            }
        }
    }

    fun onClear(view: View) {
        textInput.text = ""
        state = StateType.Empty
        emphasisBalance = 0
    }

    @SuppressLint("SetTextI18n")
    fun onEqual(view: View) {
        when (state) {
            StateType.Digit, StateType.DigitAfterDot, StateType.CloseEmphasis -> {
                for (i in 0..emphasisBalance) {
                    onREmphasis(view)
                }
                try {
                    val result = ExpressionBuilder(textInput.text.toString()).build().evaluate()
                    textInput.text = result.toString()

                    var hasDot = false
                    for (i in textInput.text) {
                        if (i == '.') {
                            hasDot = true
                            break
                        }
                    }
                    state = if (hasDot) StateType.DigitAfterDot else StateType.Digit
                } catch (e: Exception) {
                    textInput.text = "Error"
                    state = StateType.Error
                }
            }

            StateType.Error -> onClear(view)

            StateType.Operator, StateType.Dot, StateType.OpenEmphasis -> {
                onDelete(view)
                onEqual(view)
                return
            }

            StateType.Empty -> {
                textInput.text = "0"
                state = StateType.Digit
            }
        }
    }

    private fun deleteLastChar() {
        textInput.text = textInput.text.toString().substring(0, textInput.text.length - 1)
    }
}
