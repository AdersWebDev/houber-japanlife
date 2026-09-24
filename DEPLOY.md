# 배포 가이드

## 1. 운영 구조

- AWS EC2(Amazon Linux) 서버 한 대에서 nginx(HTTPS)가 요청을 받아 Spring Boot JAR(포트 40002)로 넘깁니다.
- Redis는 같은 서버, MySQL은 외부 DB, 이미지는 AWS S3를 씁니다.
- 앱은 `/home/ec2-user/japanlife/`에서 `nohup java -jar ...`로 실행됩니다. systemd 서비스가 아닙니다.
- `application.yml`의 `ENC(...)` 값은 Jasypt로 암호화돼 있습니다. 복호화 비밀번호는 저장소에 없습니다.

> ⚠️ **서버에서 빌드하지 마세요.** 서버 메모리가 작아서 빌드하는 동안 운영 중인 앱이 멈출 수 있습니다.
> 같은 이유로 **서버에 있던 예전 배포 스크립트도 쓰지 마세요.** 서버의 git 클론을 빌드해서 앱 JAR를 덮어씁니다.

---

## 2. 배포 방법

push하면 GitHub Actions(`Build JAR`)가 서버에 올릴 파일을 자동으로 만듭니다.

1. **파일 내려받기:** 저장소 **Actions** 탭 → `Build JAR` → 배포할 커밋의 실행 결과(초록색 체크)를 엽니다. **Artifacts**의 `deploy-files`를 내려받아 압축을 풀면 두 파일이 나옵니다.
   - `osaka-city-0.0.1-SNAPSHOT.jar`
   - `deploy-jar.sh`
2. **서버에 올리기:** 두 파일을 서버의 `/home/ec2-user/`(홈 폴더)에 올립니다. WinSCP나 PuTTY의 `pscp`를 쓰면 됩니다.
3. **점검:** 아래 명령은 서버를 바꾸지 않습니다. `CHECK OK: ready to deploy`가 나와야 합니다.

   ```bash
   bash ~/deploy-jar.sh --check
   ```

4. **배포:** 아래 명령을 실행합니다. `OK: new version is up`이 나오면 성공입니다.

   ```bash
   bash ~/deploy-jar.sh
   ```

   - 현재 JAR 백업 → 기존 앱 종료 → 새 JAR로 교체 → 시작 → 정상 동작 확인 순서로 진행합니다.
   - 새 버전이 정상적으로 뜨지 않으면 자동으로 이전 버전으로 되돌립니다.
   - 실행 중인 앱의 실행 명령을 재사용하므로 비밀번호를 입력하지 않습니다.
5. **master에 합치기:** 운영에서 문제가 없으면 배포한 브랜치를 master에 합칩니다.

> 재시작하는 30초~2분 동안 사이트에 `502 Bad Gateway`가 보일 수 있습니다. 방문자가 적은 시간에 배포하세요.

### 마지막 메시지별 대처

| 마지막 메시지 | 의미 | 할 일 |
|------|------|------|
| `CHECK OK: ready to deploy` | 점검 통과. 서버는 그대로 | `--check` 없이 실행해서 배포 |
| `OK: new version is up` | 배포 성공 | 없음 |
| `rolled back: previous version is up again` | 새 버전이 실패해서 이전 버전으로 복구됨. 사이트는 정상 | `~/japanlife/nohup.out.failed`로 실패 원인 확인 |
| `ROLLBACK ALSO FAILED` | 복구도 실패해서 사이트가 멈춤 | `tail -50 ~/japanlife/nohup.out`으로 원인 확인 |
| `ERROR: ...` | 시작 전 확인 단계에서 멈춤. `nothing was changed`가 있으면 서버는 그대로 | 메시지 내용 확인 |
| `$'\r': command not found`, `syntax error` | Windows에서 `deploy-jar.sh`를 열어 저장해서 줄바꿈 형식이 바뀜. 서버는 그대로 | `sed -i 's/\r$//' ~/deploy-jar.sh` 실행 후 다시 배포 |

### 이전 버전으로 되돌리기

배포할 때마다 `~/japanlife/osaka-city-0.0.1-SNAPSHOT.jar.bak-날짜-시간` 백업이 생깁니다.

```bash
ls ~/japanlife/*.bak-*        # 백업 목록 확인
cp ~/japanlife/osaka-city-0.0.1-SNAPSHOT.jar.bak-YYYYMMDD-HHMMSS ~/osaka-city-0.0.1-SNAPSHOT.jar
bash ~/deploy-jar.sh
```

### 서버가 재부팅됐을 때

앱이 자동으로 켜지지 않습니다. 아래처럼 현재 JAR로 다시 시작하세요. 스크립트가 Jasypt 비밀번호를 물으면 담당자에게 확인해서 입력합니다.

```bash
cp ~/japanlife/osaka-city-0.0.1-SNAPSHOT.jar ~/ && bash ~/deploy-jar.sh
```

---

## 3. 로컬 빌드

JDK 17을 설치한 뒤 `gradlew.bat clean bootJar`(Windows) 또는 `./gradlew clean bootJar`를 실행합니다. 결과물은 `build/libs/osaka-city-0.0.1-SNAPSHOT.jar`입니다.

`gradlew build`는 테스트가 Jasypt 비밀번호와 DB를 요구해서 실패합니다.

---

## 4. 코드 수정 시 알아둘 것

- **메인 페이지 이벤트 배너**는 DB가 아니라 `src/main/resources/templates/index.html`의 "안 챙기면 아까운 입주 혜택 확인하기" 영역에 직접 들어 있습니다.
- **게시글 노출 여부**(`post` 테이블의 `is_show`)는 관리 화면이 없어서 DB에서 직접 바꿔야 합니다.
- **`spring.jpa.hibernate.ddl-auto: update`** 때문에 엔티티 클래스를 수정하면 앱이 켜질 때 운영 DB 테이블 구조가 바뀝니다. 엔티티를 수정했다면 배포 전에 DB를 백업하세요.
- **배포한 코드는 반드시 master에 반영하세요.** 그래야 다음 사람이 같은 코드에서 작업합니다.
