# 🧠 JADE Grid Game — Multi-Agent Trail Race

This is a grid-based multi-agent game built using the **JADE** (Java Agent DEvelopment Framework). Players (agents) race toward a goal by moving step by step — but each move requires a specific colored token. If an agent lacks the right token, it can **negotiate trades** with others... or **betray** them.

## 🎮 How It Works

- Each player is a JADE agent with:
  - A starting position
  - A goal position
  - A list of tokens (colors)
- To move, a player must use a token matching the tile's color.
- If blocked, the player can initiate a **token trade** with another player.
- After **3 turns blocked**, the player is disqualified.
- The game ends when a player reaches their goal or all are blocked.

---

## ⚙️ Prerequisites

> 🧩 You must manually download and extract JADE (JADE is not on Maven/Gradle).

1. Download **JADE 4.6.0** from [https://jade.tilab.com/](https://jade.tilab.com/)
2. Extract the archive
3. Locate: `jade/lib/jade.jar`

---

## 🧪 Run the Game

### 1. Compile

**macOS/Linux**
```bash
javac -cp ".:/path/tojade/lib/jade.jar" src/*.java && java -cp ".:/Users/mery/Downloads/JADE-all-4.6.0/jade/lib/jade.jar:src" StartJADE
```
**Windows (PowerShell)**
``` bash
javac -cp ".;C:\path\to\jade\lib\jade.jar" src\*.java && java -cp ".;C:\path\to\jade\lib\jade.jar;src" StartJADE    
```

Replace /path/to/jade/ with your actual JADE path.

### 2. Run
```
java -cp ".:/path/to/jade/lib/jade.jar" agents.StartJADE
```

## 💡 Ideas for Improvement

- Smarter negotiation logic
- Fair trading incentives (avoid betrayals?)
- RL behavior customization
- GUI scoreboard and history log
