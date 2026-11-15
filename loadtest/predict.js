// Load test for POST /predict.
// Run: docker run --rm -i --network host -v "$PWD/loadtest:/scripts" grafana/k6 run /scripts/predict.js
import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://plant-api.10.212.51.11.nip.io";
const image = open("./leaf.jpg", "b");

export const options = {
  scenarios: {
    predict: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 5 },   // warm up
        { duration: "1m", target: 20 },   // ramp up: HPA should start adding pods
        { duration: "2m", target: 20 },   // sustained load
        { duration: "30s", target: 0 },   // ramp down
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],                       // < 1% errors
    "http_req_duration{endpoint:predict}": ["p(95)<800"], // 95% of requests under 800 ms
    checks: ["rate>0.99"],
  },
};

export default function () {
  const res = http.post(
    `${BASE_URL}/predict`,
    { image: http.file(image, "leaf.jpg", "image/jpeg") },
    { tags: { endpoint: "predict" } },
  );
  check(res, {
    "status is 200": (r) => r.status === 200,
    "plant returned": (r) => r.status === 200 && r.json("plant.id") !== undefined,
  });
  sleep(0.5);
}
