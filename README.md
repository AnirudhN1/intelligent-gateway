# 🛡️ SentryGate  
### Intelligent Load-Shedding API Gateway

> **SentryGate** is an adaptive, AI-driven API Gateway designed to protect microservices from cascading failures and "Thundering Herd" scenarios.  
> It combines **Fuzzy Logic** and **LSTM Neural Networks** to dynamically regulate traffic based on real-time and predicted system load.

---

## 📸 Dashboard Preview
<img width="1459" height="812" alt="Screenshot 2026-04-19 131225" src="https://github.com/user-attachments/assets/fa2c057c-f736-4f2c-9c52-2693bb77d7c1" />

---

## ✨ Features

- 🧠 **Proactive AI (LSTM)** Predicts CPU spikes **5–10 seconds in advance** using DeepLearning4j.

- ⚡ **Dynamic Load Shedding** Uses **Fuzzy Logic** to continuously adjust request limits based on system stress.

- 📊 **Real-time Telemetry Dashboard** Visualizes:
  - Actual CPU usage
  - AI predictions
  - Dynamic rate limits

- 📈 **Trend Amplification Engine** Detects attack slopes and reacts **before system saturation**.

- ⚙️ **Reactive & Non-blocking Architecture** Built with **Spring WebFlux** for high concurrency.

---

## 🏗️ Architecture

SentryGate operates as an intelligent control layer at the edge of your infrastructure. It intercepts all incoming traffic and evaluates it against a dynamic threshold using four core components:

1. **ResourceMonitor:** Samples system vitals (CPU/Memory) every 1 second and maintains a concurrent sliding window of the last 5 seconds.
2. **LSTMPredictor:** Ingests the sliding window to output a trend-amplified forecast of upcoming system stress.
3. **FuzzyController:** Maps "Crisp" CPU values (real or predicted) into linguistic variables (Low, Medium, High Stress) to calculate a weighted average shedding limit.
4. **Redis Quota Manager:** A high-performance reactive store that tracks request counts against the dynamic limits.

---

## 🐳 Infrastructure: The Redis Sidecar Pattern

SentryGate is designed for distributed environments using the **Sidecar Pattern**. The gateway acts as the ingress node, utilizing a dedicated Redis sidecar container for low-latency quota coordination.

By utilizing a Redis sidecar, SentryGate ensures:
- **Atomic Incrementing:** Prevents race conditions during heavy concurrent "Thundering Herd" attacks.
- **Statelessness:** The Gateway can be scaled horizontally while maintaining a global view of the current ingress quota.
- **Low Latency:** Sub-millisecond lookup times for quota verification, ensuring the AI inference remains the only significant part of the request overhead.

### Docker Compose Orchestration

To run the backing infrastructure, a `docker-compose.yml` is provided:

```yaml
version: '3.8'
services:
  sentry-redis:
    image: redis:7-alpine
    container_name: sentry_redis_node
    ports:
      - "6379:6379"
    networks:
      - sentry_net

  sentry-gateway:
    build: .
    container_name: sentry_gateway_app
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_REDIS_HOST=sentry-redis
    depends_on:
      - sentry-redis
    networks:
      - sentry_net

networks:
  sentry_net:
    driver: bridge
```

---

## 🧠 The AI Pipeline

SentryGate follows a "Collect-Train-Deploy" lifecycle to ensure the AI is calibrated to the specific hardware it protects.

1. **Data Collection:** The system captures the raw "physics" of the hardware under stress (saved to `telemetry.csv` in 1-second intervals).
2. **Training:** The `ModelTrainer` processes the time-series data into a many-to-one LSTM architecture. The objective is to minimize Mean Squared Error (MSE).
3. **Inference:** The gateway uses the trained model to perform non-blocking inference. By calculating the trend (Delta), the prediction is amplified to be proactive: `Acting CPU = Current CPU + (Delta * BoostFactor)`.

---

## 🛠️ Tech Stack

| Component | Technology |
| :--- | :--- |
| **Framework** | Spring Boot 3.x, Spring Cloud Gateway |
| **Runtime** | Java 17 |
| **Machine Learning** | DeepLearning4j (DL4J), ND4J |
| **Data Store** | Redis (Reactive WebFlux) |
| **Monitoring** | Micrometer, Actuator |
| **Frontend** | HTML5, Chart.js, Vanilla JS |

---

## 🚦 Getting Started

### Prerequisites
* JDK 17 or higher
* Docker & Docker Compose
* Maven

### Installation & Execution

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/your-username/sentrygate.git](https://github.com/your-username/sentrygate.git)
   cd sentrygate
   ```

2. **Start the Redis Sidecar:**
   ```bash
   docker-compose up -d sentry-redis
   ```

3. **Build the Gateway:**
   ```bash
   mvn clean install
   ```

4. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the Dashboard:**
   Open your browser and navigate to: `http://localhost:8080/index.html`

---

## 📊 Fuzzy Logic Math

The system calculates the dynamic limit (L) using the Weighted Average of Center of Sums:

```text
L = Σ(μ_i * C_i) / Σ(μ_i)
```

Where:
* `μ_i` is the degree of membership in a stress set (Calculated via Trapezoidal/Triangular overlap functions).
* `C_i` is the crisp singleton output value representing the limit for that state (e.g., 5, 20, or 50 requests/sec).

---

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.
