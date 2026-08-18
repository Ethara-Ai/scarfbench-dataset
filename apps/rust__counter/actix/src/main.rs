// FULL-fidelity
use actix_web::{App, HttpServer};


async fn h_counter(counter: actix_web::web::Data<std::sync::atomic::AtomicU64>) -> impl actix_web::Responder {
    let n = counter.fetch_add(1, std::sync::atomic::Ordering::SeqCst) + 1;
    actix_web::HttpResponse::Ok().content_type("text/plain; charset=utf-8").body(format!("accessed {} time(s)", n))
}

async fn h_1() -> impl actix_web::Responder {
    actix_web::HttpResponse::Ok().content_type("text/html; charset=utf-8").body("OK")
}


#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let counter = actix_web::web::Data::new(std::sync::atomic::AtomicU64::new(0));
    println!("Actix counter on 8080");
    HttpServer::new(move || {
        App::new()
            .app_data(counter.clone())
            .route("/counter", actix_web::web::get().to(h_counter))
            .route("/", actix_web::web::get().to(h_1))
    })
    .bind("0.0.0.0:8080")?
    .run()
    .await
}
