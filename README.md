# Talksy — Chat JMS com ActiveMQ

O **Talksy** é um chat multiusuário estilo “bate-papo UOL” desenvolvido em **Java (Swing + JMS)**, utilizando **ActiveMQ** como broker de mensagens.  
Suporta:
- Sala pública (Topic)
- Mensagens privadas (Queue)
- Lista de usuários online
- Indicação de quantos usuários estão conectados

---

## Requisitos

- **Sistema Operacional**: Windows 10/11 (testado), Linux ou macOS também funcionam
- **Java JDK**: 11 ou superior (projeto usa Java 19 nos testes)
- **Apache Maven**: 3.6+ (foi usado 3.9.11)
- **Apache ActiveMQ (Classic)**: 5.18.x ou versão próxima

---

## Instalação dos requisitos

### 1. Java JDK
1. Baixe em: [https://adoptium.net/](https://adoptium.net/) ou [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)  
2. Instale normalmente.  
3. Teste no terminal/PowerShell:
   ```powershell
   java -version
   ```
   Deve aparecer algo como `java version "19.0.2"`.

---

### 2. Apache Maven
1. Baixe em: [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)  
2. Extraia o `.zip` em `C:\apache-maven-3.9.11` (ou versão mais recente).  
3. Configure as variáveis de ambiente:
   - `MAVEN_HOME = C:\apache-maven-3.9.11`
   - Adicione no **Path**:
     ```
     C:\apache-maven-3.9.11\bin
     ```
4. Teste no terminal:
   ```powershell
   mvn -v
   ```
   Deve mostrar a versão do Maven.

---

### 3. Apache ActiveMQ
1. Baixe o **ActiveMQ Classic**: [https://activemq.apache.org/components/classic/download](https://activemq.apache.org/components/classic/download)  
2. Extraia em `C:\apache-activemq-5.18.3`  
3. Para iniciar o broker:
   ```powershell
   cd C:\apache-activemq-5.18.3\bin
   .\activemq.bat start
   ```
4. Para verificar se subiu, abra no navegador:
   ```
   http://localhost:8161/
   ```
   Login: `admin / admin`

---

## Como compilar e executar o Talksy

### 1. Compilar
```powershell
mvn clean compile
```

### 2. Executar
```powershell
mvn exec:java
```

Isso abre a janela do Talksy (Swing GUI).

---

## Testando com múltiplos usuários

1. Deixe o **ActiveMQ rodando**.  
2. Em um terminal, rode:
   ```powershell
   mvn exec:java
   ```
   → Digite um apelido, ex.: `Biia` 
   → Depois, digite o IP do servidor, ex.: 192.168.1.19 
3. Abra outro terminal e rode de novo:
   ```powershell
   mvn exec:java
   ```
   → Digite outro apelido, ex.: `Lucas` 
   → Digite o mesmo IP do servidor, ex.: 192.168.1.19  
4. Agora você pode trocar mensagens públicas e privadas entre as janelas.

---

## Executando em rede (múltiplos computadores)

**No servidor (onde o AcitiveMQ vai rodar):**

1. Suba o ActiveMQ:  
   ```powershell
   activemq start
   ```

2. Descubra o IP local:
   ```powershell
   ip config
   ```
   → Anote o IPv4, por exemplo: 192.168.1.19

3. Garanta que a porta 61616 está liberada no firewall:
   - Abra o Menu Iniciar → digite Firewall do Windows com Segurança Avançada → abra.
   - Clique em Regras de Entrada → Nova Regra....
   - Selecione Porta → Avançar.
   - Escolha TCP e em Portas locais específicas digite: 61616
   - Clique em Avançar → selecione Permitir a conexão.
   - Marque os perfis (Domínio, Privado, Público) → Avançar.
   - Nomeie a regra, ex: ActiveMQ 61616
   - Clique em Concluir.

**Nos clientes (outros PCs da rede):**
1. Ao iniciar o Talksy, informe:
   - Nome de usuário (apelido)
   - IP do servidor (onde o ActiveMQ está rodando)

---

## Estrutura do projeto

```
Talksy/
 ├─ pom.xml
 └─ src/
    └─ main/
       └─ java/
          └─ com/example/talksy/
             ├─ TalksyUI.java   # Interface Swing
             └─ TalksyChat.java # Lógica JMS
```

---

## Autores
Projeto acadêmico desenvolvido em Java, com ActiveMQ (JMS) por Beatriz Braga Silva e Lucas Silva Cardoso dos Santos
