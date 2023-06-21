#  File-server

---

클라이언트는 ****Android Studio****, 서버는 ****IntelliJ****를 사용하였다. java버전은 17.0.6이다. 

### 결과화면

https://s3-us-west-2.amazonaws.com/secure.notion-static.com/5df4ba24-3666-41f6-908d-a6dc30d37a91/Screenshot_20230622_024836.png

서버에서  받은  모든  파일을 갤러리뷰 형식으로  보여준다.  
이미지 파일이면  이미지를  자체를  보여주고, 텍스트나 zip  파일이면  임의로  선정한  icon  image를띄워서  보여준다.

### 요구사항에 따른 코드 설명

<aside>
💡 a. 클라이언트는 서버에 모든 파일 정보를 요청한다.

</aside>

`FileFetcher.java`

```java
private String requestFileInfo()  throws IOException {
    HttpURLConnection connection = createConnection(SERVER_URL);
    connection.setRequestMethod("GET");
    Log.i("requestFileInfo", "connection success");

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
        InputStream inputStream = connection.getInputStream();
        String response = readResponse(inputStream);
        inputStream.close();
        connection.disconnect();
        return response;
    } else {
        String errorMessage = handleErrorResponseCode(responseCode);
        handleErrorResponse(errorMessage);
        return null;
    }
}
```

- MainActivity가 onCreate()될 때, 비동기 작업을 수행하는 스레드 테스크 `FetchFilesTask` 를 만들어 서버에 모든 파일 정보를 요청하는 requestFileInfo() 메소드를 실행시킨다.
- requestFileInfo() 메소드는 단순히 서버로부터 지정된 API형식으로 파일 정보를 요청하여 모든 파일 정보가 담겨 있는 응답값을 String형태로 리턴해 준다.

<aside>
💡 b.  서버는  자신이  저장하고  있는  파일  정보를  모두  전달한다.  파일정보란  파일
이름,  파일  크기,  파일  타입을  의미한다.  서버는  최소  두개  이상의 이미지/텍스트/zip  파일을  가지고  있다.

</aside>

`HTTPServer.java`

```java
// API: /files
this.server.createContext("/files", exchange -> {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"data\": [");

    File[] images = FileManager.getFilesInDirectory("images");
    File[] texts = FileManager.getFilesInDirectory("texts");
    File[] zips = FileManager.getFilesInDirectory("zips");

    generateJSONStringBy(images, "image", sb, false);
    generateJSONStringBy(texts, "text", sb, false);
    generateJSONStringBy(zips, "zip", sb, true);

    sb.append("]");
    sb.append("}");

    byte[] result = sb.toString().getBytes(StandardCharsets.UTF_8);

    OutputStream res = exchange.getResponseBody();
    Headers responseHeader = exchange.getResponseHeaders();
    responseHeader.add("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(200, result.length);
    res.write(result);

    res.flush();
    res.close();
    exchange.close();
});
```

- Main.java에서 호출하는 `HTTPServer` 생성자 메소드 안 코드이다. 생성자를 호출하여 서버를 만들때 API를 설정하고 파일을 불러와 파일정보(파일 이름,  파일  크기,  파일  타입)를 JSON형태의 String으로 만들어 준다.

https://s3-us-west-2.amazonaws.com/secure.notion-static.com/ebc42eb3-d7fc-43b1-8381-9428cf5232fd/Untitled.png

서버 내부에 두개 이상의 이미지/텍스트/zip파일을 각각의 폴더에 가지고 있다.

<aside>
💡 c. 클라이언트는  각  파일의  정보를  가지고  차례대로  파일을  서버에게  http프로토콜에  맞춰  요청한다.
e. 클라이언트는 이렇게 받는 모든 파일을 갤러리 뷰를 이용하여 보여준다.

</aside>

`GalleryAdapter.java`

```java
private void loadImageFromServer(String fileName, String fileType, ImageView imageView) {
    // 애뮬레이터의 로컬 호스트 IP는 127.0.0.1, 때문에 서버 URL을 따로 표기해 주어야 함.
    String imageUrl = "http://121.133.180.56:4000/file/" + fileName + "?type=" + fileType;

    ImageLoadTask imageLoadTask = new ImageLoadTask(imageView);
    imageLoadTask.execute(imageUrl);
}
```

`ImageLoadTask.java`

```java
@Override
  protected Bitmap doInBackground(String... params) {
      String imageUrl = params[0];

      try {
          URL url = new URL(imageUrl);
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setDoInput(true);
          connection.connect();

          InputStream input = connection.getInputStream();
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = input.read(buffer)) != -1) {
              byteArrayOutputStream.write(buffer, 0, bytesRead);
          }
          byte[] imageBytes = byteArrayOutputStream.toByteArray();

          return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
      } catch (IOException e) {
          e.printStackTrace();
      }

      return null;
  }
```

- 이미지 파일의 경우에 `FileFetcher` 에서 파일정보요청 리퀘스트가 끝나면 리스너로 등록한 MainActivity에서 `onFilesFetched()` 메소드를 호출시킨다
- 이때 파일 이미지등을 MainActivity내부의 RecyclerView에서 갤러리 형태로 보여주기 위해 `GalleryAdapter` 를 호출한다.
- `GalleryAdapter` 는 각각의 파일에 맞는 텍스트(파일이름), 이미지(이미지 자체 혹은 아이콘)를 바인딩 한다.
- 바인딩 할때 파일 타입이 `image` 이면 `loadImageFromServer()` 메소드에서 이미지로드를 위한 비동기 스레트 테스크인 `ImageLoadTask` 를 실행시켜 이미지를 byte단위로 읽어와 디코딩한다.
- 디코딩한 `Bitmap` 을 이미지 뷰에 세팅하여 보여준다.

<aside>
💡 d. 서버는 클라이언트가 요청하는 파일을 http 프로토콜에 맞춰 전달한다.

</aside>

`HTTPServer.java`

```java
// API: /file/{ file-name }?type={ image | text | zip }
this.server.createContext("/file", exchange -> {
    String[] paths = exchange.getRequestURI().getPath().split("/");
    String fileName = paths[paths.length - 1];

    String[] typeAndValue = exchange.getRequestURI().getQuery().split("=");
    String type = typeAndValue[typeAndValue.length - 1];

    switch (type) {
        case "image" -> this.getImageFileAndResponse(fileName, exchange);
        case "text" -> this.getTextFileAndResponse(fileName, exchange);
        case "zip" -> this.getZipFileAndResponse(fileName, exchange);
    }
});
```

- 이것 또한 Main.java에서 호출하는 `HTTPServer` 생성자 메소드 안 코드이다. 생성자를 호출하여 서버를 만들때 파일을 하나씩 받을 수 있는 API를 설정하였다.
- 파일이름을 URL에 포함하고 타입을 쿼리형태로 날리도록 한다.
- createContext() 메소드 안에 두번째 파라미터는 들어오는 요청에 대해 호출할 핸들러를 람다식으로 전달한 것이다.
- 람다식 핸들러 안의 exchange는 수신된 HTTP 요청과 생성될 response를 하나로 캡슐화한것이다. 이를  `getImageFileAndResponse()` 메소드안에 다시 전달하여 클라이언트의 요청을 검사하고  response를 작성 및 전송한다.
