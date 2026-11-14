import AppKit
import WebKit
import PDFKit

let htmlURL = URL(fileURLWithPath: CommandLine.arguments[1])
let outURL = URL(fileURLWithPath: CommandLine.arguments[2])
let pages = Int(CommandLine.arguments[3])!
let W: CGFloat = 794, H: CGFloat = 1123

class Renderer: NSObject, WKNavigationDelegate {
    let web = WKWebView(frame: NSRect(x: 0, y: 0, width: W, height: H * CGFloat(pages)))
    let doc = PDFDocument()
    func start() {
        web.navigationDelegate = self
        web.loadFileURL(htmlURL, allowingReadAccessTo: htmlURL.deletingLastPathComponent())
    }
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { self.render(0) }
    }
    func render(_ i: Int) {
        if i == pages {
            doc.documentAttributes = [PDFDocumentAttribute.titleAttribute: "HeatCall – esite"]
            doc.write(to: outURL); print("ok \(doc.pageCount) pages"); exit(0)
        }
        let cfg = WKPDFConfiguration()
        cfg.rect = CGRect(x: 0, y: H * CGFloat(i), width: W, height: H)
        web.createPDF(configuration: cfg) { result in
            switch result {
            case .success(let data):
                if let p = PDFDocument(data: data)?.page(at: 0) { self.doc.insert(p, at: self.doc.pageCount) }
                self.render(i + 1)
            case .failure(let e): print("error \(e)"); exit(1)
            }
        }
    }
}
let app = NSApplication.shared
let r = Renderer()
r.start()
app.run()
