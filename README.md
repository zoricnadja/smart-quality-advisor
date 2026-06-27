# smart-quality-advisor
Smart Quality Advisor is a knowledge-based system that helps small food producers monitor product quality and safety. It analyzes real-time sensor data and user inputs using rule-based reasoning, detects risks, provides alerts and recommendations, and explains decisions to ensure safe and consistent production.

## Demonstration

Run the Spring Boot service and open the browser client at `http://localhost:8080/`.

Available API demonstrations:

- `GET /api/quality/demo` - full forward chaining demonstration.
- `GET /api/quality/cep-demo` - CEP aggregation and chained event demonstration.
- `GET /api/quality/cep-pseudo-clock-demo` - CEP demonstration with a pseudo clock for defense scenarios.
- `GET /api/quality/template-demo` - Drools template demonstration for generated salt rules.
- `GET /api/quality/backward-demo` - backward chaining explanation for blocked batches.
