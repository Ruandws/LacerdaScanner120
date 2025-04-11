<h1 align="center">📱 LacerdaScanner120</h1>
<p align="center">Leitor de QR Codes com Histórico e Exportação para CSV</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.0-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-✔️-blue?logo=android" />
  <img src="https://img.shields.io/badge/ZXing-integrado-success?logo=barcode" />
  <img src="https://img.shields.io/badge/License-MIT-green" />
</p>

---

## 🧭 Visão Geral

O **LacerdaScanner120** é um app Android construído com **Kotlin** e **Jetpack Compose**, projetado para ler QR Codes, salvar automaticamente o histórico de leituras e exportar os dados em formato **`.csv e .xlsx`**.  
Ideal para quem precisa registrar e organizar informações rapidamente, com praticidade e um toque moderno.

---

## ✨ Funcionalidades

| Função                     | Descrição                                                                 |
|----------------------------|---------------------------------------------------------------------------|
| 📷 Leitura de QR Codes     | Escaneia códigos em tempo real com a biblioteca ZXing.                   |
| 📝 Histórico Automático    | Armazena os QR Codes lidos em uma lista atualizada dinamicamente.        |
| 🧹 Limpeza de Histórico     | Permite apagar todos os registros com um clique.                         |
| 📤 Exportação para CSV     | Salva o histórico em arquivo `.csv` no armazenamento local do dispositivo.|
| 🔐 Permissões Dinâmicas    | Gerencia as permissões da câmera com tratamento elegante para o usuário. |

---

## 🛠️ Tecnologias Utilizadas

- **Kotlin** • Linguagem moderna e concisa para Android  
- **Jetpack Compose** • UI declarativa e reativa  
- **ZXing (JourneyApps)** • Leitura de QR Codes  
- **Material 3** • Componentes visuais modernos  
- **Android SDK** • Integração com permissões e armazenamento

---

## 🧩 Estrutura do App

- Interface construída com `Scaffold`, `BottomAppBar`, `LazyColumn`
- Gerenciamento de estados via `mutableStateOf` e `mutableStateListOf`
- Exportação de dados via `FileOutputStream` para `.csv`
- Modularizado para fácil expansão e manutenção

---


## 🎥 Demonstração em Vídeo

https://github.com/user-attachments/assets/a62078da-3083-48c0-a1d4-c6ead25d472c


---
## 🤝 Contribuindo

Contribuições são bem-vindas!  
Se você tiver ideias, sugestões ou quiser adicionar funcionalidades, fique à vontade para abrir uma issue ou pull request.  

---

## 📄 Licença

Distribuído sob a licença **MIT**.  
Consulte o arquivo `LICENSE` para mais detalhes.

---

### 🚀 Feito com 💻 por [Ruan Andrade]

