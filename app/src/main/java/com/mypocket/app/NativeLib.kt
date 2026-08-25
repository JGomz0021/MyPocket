package com.mypocket.app

class NativeLib {
    external fun formatCurrency(amount: Double): String
    
    companion object {
        init {
            try {
                System.loadLibrary("pocket_rust")
            } catch (e: UnsatisfiedLinkError) {
                // Fallback si no está compilado el bridge de Rust aún
            }
        }
    }
    
    // Fallback estático solicitado si el bridge falla o para el MVP rápido
    fun formatCurrencyFallback(amount: Double): String {
        return "$ ${String.format("%.2f", amount)}"
    }
}
