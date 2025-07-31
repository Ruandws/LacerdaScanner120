package com.example.lacerdascanner120

class DataModels {
    // Modelagem de Dados
    data class Contrato(val id: Int, val nome: String)
    data class Anel(val id: Int, val nome: String, val contratoId: Int)
    data class Posto(val id: Int, val nome: String, val anelId: Int)
}