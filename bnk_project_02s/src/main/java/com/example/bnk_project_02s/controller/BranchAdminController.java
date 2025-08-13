package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.service.BranchImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/branches")
public class BranchAdminController {

    private final BranchImportService importService;

    /** ① 클래스패스 CSV로 일괄 등록 */
    @PostMapping("/import-classpath")
    @ResponseBody
    public String importFromClasspath() throws Exception {
        int n = importService.importFromClasspathCsv();
        return "imported rows = " + n;
    }

    /** ② 파일 업로드로 CSV 등록 */
    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public String importFromUpload(@RequestPart("file") MultipartFile file) throws Exception {
        int n = importService.importFromCsvInputStream(file.getInputStream());
        return "imported rows = " + n;
    }

    /** ③ DB에 이미 있는 데이터 중 좌표 NULL만 지오코딩해서 업데이트 */
    @PostMapping("/geocode")
    @ResponseBody
    public String fillMissingCoords() {
        int n = importService.fillMissingCoordsFromDb();
        return "updated rows = " + n;
    }
}
