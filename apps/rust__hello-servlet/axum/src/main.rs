// FULL-fidelity
use axum::{Router, response::{Html, IntoResponse, Json}, routing::{get, post}, http::StatusCode};
use serde_json;
use std::sync::Arc;
use std::sync::atomic::AtomicU64;

async fn counter_handler(axum::extract::State(counter): axum::extract::State<std::sync::Arc<std::sync::atomic::AtomicU64>>) -> impl IntoResponse {
    let n = counter.fetch_add(1, std::sync::atomic::Ordering::SeqCst) + 1;
    (StatusCode::OK, [(axum::http::header::CONTENT_TYPE, "text/plain; charset=utf-8")], format!("accessed {} time(s)", n))
}

fn shift(s: &str) -> String {
    let cin: Vec<char> = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().collect();
    let cout: Vec<char> = "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA".chars().collect();
    s.chars().map(|c| match cin.iter().position(|&x| x==c) { Some(i)=>cout[i], None=>c }).collect()
}

async fn cipher_get_handler(axum::extract::Query(q): axum::extract::Query<std::collections::HashMap<String,String>>) -> impl IntoResponse {
    let s = q.get("inputString").cloned().unwrap_or_default();
    (StatusCode::OK, [(axum::http::header::CONTENT_TYPE, "text/plain; charset=utf-8")], format!("Coded: {}", shift(&s)))
}
async fn greeting_handler(axum::extract::Query(q): axum::extract::Query<std::collections::HashMap<String,String>>) -> impl IntoResponse {
    let n = q.get("name").cloned().unwrap_or_else(|| "World".to_string());
    (StatusCode::OK, [(axum::http::header::CONTENT_TYPE, "text/html; charset=utf-8")], format!("Hello, {}", n))
}
async fn lower_get_handler(axum::extract::Query(q): axum::extract::Query<std::collections::HashMap<String,String>>) -> impl IntoResponse {
    let n = q.get("name").cloned().unwrap_or_else(|| "world".to_string());
    (StatusCode::OK, [(axum::http::header::CONTENT_TYPE, "text/html; charset=utf-8")], format!("Hello, {}", n.to_lowercase()))
}


#[tokio::main]
async fn main() {
    let counter = Arc::new(AtomicU64::new(0));
    let app = Router::new()
        .route("/greeting", get(greeting_handler))
        .route("/", get(|| async { Html("Hello, World") }))
        .with_state(counter);
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    println!("Axum hello-servlet on 8080");
    axum::serve(listener, app).await.unwrap();
}
