use komi_plugin as komi;
use base64::Engine;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

mod komi;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpReq {
    pub url: String,
    pub headers: HashMap<String, String>,
    pub body: Option<String>,
    pub timeout_ms: Option<u64>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpResp {
    pub status: u16,
    pub headers: HashMap<String, String>,
    pub body_b64: String,
}

async fn client(timeout_ms: u64) -> Result<reqwest::Client, String> {
    reqwest::Client::builder()
        .timeout(std::time::Duration::from_millis(timeout_ms))
        .build()
        .map_err(|e| e.to_string())
}

async fn to_resp(resp: reqwest::Response) -> Result<HttpResp, String> {
    let status = resp.status().as_u16();
    let mut headers = HashMap::new();
    for (k, v) in resp.headers() {
        headers.insert(k.to_string(), v.to_str().unwrap_or("").to_string());
    }
    let bytes = resp.bytes().await.map_err(|e| e.to_string())?;
    Ok(HttpResp {
        status,
        headers,
        body_b64: base64::engine::general_purpose::STANDARD.encode(&bytes),
    })
}

#[tauri::command]
async fn http_post(req: HttpReq) -> Result<HttpResp, String> {
    let client = client(req.timeout_ms.unwrap_or(300000)).await?;
    let mut rb = client.post(&req.url);
    for (k, v) in &req.headers {
        rb = rb.header(k.as_str(), v.as_str());
    }
    if let Some(b) = &req.body {
        rb = rb.body(b.clone());
    }
    let resp = rb.send().await.map_err(|e| format!("network: {}", e))?;
    to_resp(resp).await
}

#[tauri::command]
async fn http_get(req: HttpReq) -> Result<HttpResp, String> {
    let client = client(req.timeout_ms.unwrap_or(120000)).await?;
    let mut rb = client.get(&req.url);
    for (k, v) in &req.headers {
        rb = rb.header(k.as_str(), v.as_str());
    }
    let resp = rb.send().await.map_err(|e| format!("network: {}", e))?;
    to_resp(resp).await
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImagePart {
    pub mime: String,
    pub data_b64: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MultipartReq {
    pub url: String,
    pub headers: HashMap<String, String>,
    pub fields: HashMap<String, String>,
    pub images: Vec<ImagePart>,
    pub timeout_ms: Option<u64>,
}

#[tauri::command]
async fn http_multipart(req: MultipartReq) -> Result<HttpResp, String> {
    let client = client(req.timeout_ms.unwrap_or(300000)).await?;
    let mut form = reqwest::multipart::Form::new();
    for (k, v) in &req.fields {
        form = form.part(k.to_string(), reqwest::multipart::Part::text(v.clone()));
    }
    for (i, im) in req.images.iter().enumerate() {
        let data = base64::engine::general_purpose::STANDARD
            .decode(&im.data_b64)
            .map_err(|e| e.to_string())?;
        let mime = im.mime.clone();
        form = form.part(
            "image".to_string(),
            reqwest::multipart::Part::bytes(data)
                .file_name(format!("ref{}.png", i))
                .mime_str(&mime)
                .map_err(|e| e.to_string())?,
        );
    }
    let mut rb = client.post(&req.url);
    for (k, v) in &req.headers {
        rb = rb.header(k.as_str(), v.as_str());
    }
    let resp = rb.multipart(form).send().await.map_err(|e| format!("network: {}", e))?;
    to_resp(resp).await
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(komi::init())
        .invoke_handler(tauri::generate_handler![http_post, http_get, http_multipart])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
