// FULL-fidelity
use axum::{Router, response::{Html, IntoResponse, Json}, routing::{get, post}, http::StatusCode};
use serde_json;
use std::sync::Arc;
use std::sync::atomic::AtomicU64;

async fn counter_handler(axum::extract::State(counter): axum::extract::State<std::sync::Arc<std::sync::atomic::AtomicU64>>) -> impl IntoResponse {
    let n = counter.fetch_add(1, std::sync::atomic::Ordering::SeqCst) + 1;
    (StatusCode::OK, [(axum::http::header::CONTENT_TYPE, "text/plain; charset=utf-8")], format!("accessed {} time(s)", n))
}


#[tokio::main]
async fn main() {
    let counter = Arc::new(AtomicU64::new(0));
    let app = Router::new()
        .route("/contacts", get(|| async { Json(serde_json::Value::Array(vec![])) }))
        .route("/", get(|| async { Html("OK") }))
        .with_state(counter);
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    println!("Axum address-book on 8080");
    axum::serve(listener, app).await.unwrap();
}
