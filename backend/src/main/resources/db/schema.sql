
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `safedesk` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `safedesk`;
DROP TABLE IF EXISTS `abuse_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `abuse_log` (
  `abuse_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '폭언 기록 ID',
  `message_id` bigint NOT NULL COMMENT '메시지 ID',
  `consultation_id` bigint NOT NULL COMMENT '상담 ID',
  `original_content` text NOT NULL COMMENT '원문 내용',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기록 일시',
  PRIMARY KEY (`abuse_log_id`),
  UNIQUE KEY `uk_abuse_log_message_id` (`message_id`),
  KEY `fk_abuse_log_consultation` (`consultation_id`),
  CONSTRAINT `fk_abuse_log_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation` (`consultation_id`),
  CONSTRAINT `fk_abuse_log_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`message_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='폭언 원문';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `abuse_log` WRITE;
/*!40000 ALTER TABLE `abuse_log` DISABLE KEYS */;
INSERT INTO `abuse_log` VALUES (1,8,1,'시발 아직도 처리 안 됐어요?','2026-09-10 13:06:10'),(2,11,1,'병신 같은 답변 좀 그만하세요.','2026-09-10 13:08:03'),(3,20,2,'개새끼들 일 똑바로 안 해요?','2026-09-25 10:36:10'),(4,31,3,'지랄하지 말고 빨리 처리해요.','2026-10-20 11:06:10'),(5,34,3,'존나 답답하네 진짜.','2026-10-20 11:08:03'),(6,43,4,'ㅅㅂ 몇 번을 말해야 돼요?','2026-11-02 15:06:10'),(7,46,4,'시발 아직도 처리 안 됐어요?','2026-11-02 15:08:03'),(8,59,6,'병신 같은 답변 좀 그만하세요.','2026-11-03 17:26:10'),(9,62,6,'존나 답답하네 진짜.','2026-11-03 17:28:03'),(10,71,7,'지랄하지 말고 빨리 처리해요.','2026-11-04 12:06:10'),(11,74,7,'ㅅㅂ 몇 번을 말해야 돼요?','2026-11-04 12:08:03'),(12,83,8,'존나 답답하네 진짜.','2026-11-05 16:06:10'),(13,86,8,'시발 아직도 처리 안 됐어요?','2026-11-05 16:08:03'),(14,104,10,'존나 답답하네 진짜.','2026-11-07 11:06:10');
/*!40000 ALTER TABLE `abuse_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `banned_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banned_word` (
  `word_id` bigint NOT NULL AUTO_INCREMENT COMMENT '금칙어 ID',
  `word` varchar(50) NOT NULL COMMENT '금칙어',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`word_id`),
  UNIQUE KEY `uk_banned_word_word` (`word`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='금칙어';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `banned_word` WRITE;
/*!40000 ALTER TABLE `banned_word` DISABLE KEYS */;
INSERT INTO `banned_word` VALUES (1,'시발','2026-11-11 16:48:36'),(2,'시 발','2026-11-11 16:48:36'),(3,'씨발','2026-11-11 16:48:36'),(4,'씨 발','2026-11-11 16:48:36'),(5,'ㅅㅂ','2026-11-11 16:48:36'),(6,'ㅆㅂ','2026-11-11 16:48:36'),(7,'ㅆ발','2026-11-11 16:48:36'),(8,'ㅆ!발','2026-11-11 16:48:36'),(9,'씨X','2026-11-11 16:48:36'),(10,'ㅂㅅ','2026-11-11 16:48:36'),(11,'병신','2026-11-11 16:48:36'),(12,'병 신','2026-11-11 16:48:36'),(13,'지랄','2026-11-11 16:48:36'),(14,'ㅈㄹ','2026-11-11 16:48:36'),(15,'존나','2026-11-11 16:48:36'),(16,'졸라','2026-11-11 16:48:36'),(17,'개같','2026-11-11 16:48:36'),(18,'개새끼','2026-11-11 16:48:36'),(19,'개 새끼','2026-11-11 16:48:36'),(20,'새끼','2026-11-11 16:48:36'),(21,'미친','2026-11-11 16:48:36');
/*!40000 ALTER TABLE `banned_word` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `consultation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consultation` (
  `consultation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '상담 ID',
  `customer_id` bigint NOT NULL COMMENT '고객 ID',
  `agent_id` bigint DEFAULT NULL COMMENT '상담원 ID',
  `policy_id` bigint NOT NULL COMMENT '적용 정책 ID',
  `status` varchar(20) NOT NULL COMMENT '상담 상태',
  `inquiry_category` varchar(50) DEFAULT NULL COMMENT '문의 유형',
  `inquiry_summary` varchar(100) DEFAULT NULL COMMENT '문의 요약',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상담 시작 일시',
  `queued_at` datetime DEFAULT NULL COMMENT '연결 요청 일시',
  `accepted_at` datetime DEFAULT NULL COMMENT '상담원 수락 일시',
  `closed_at` datetime DEFAULT NULL COMMENT '종료 일시',
  `closed_by` varchar(20) DEFAULT NULL COMMENT '종료 주체',
  `close_reason` varchar(20) DEFAULT NULL COMMENT '종료 사유',
  `process_summary` text COMMENT '처리 내용 요약',
  `result` varchar(20) DEFAULT NULL COMMENT '처리 결과',
  PRIMARY KEY (`consultation_id`),
  KEY `fk_consultation_customer` (`customer_id`),
  KEY `fk_consultation_agent` (`agent_id`),
  KEY `fk_consultation_policy` (`policy_id`),
  KEY `ix_consultation_status_queued` (`status`,`queued_at`),
  CONSTRAINT `fk_consultation_agent` FOREIGN KEY (`agent_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_consultation_customer` FOREIGN KEY (`customer_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_consultation_policy` FOREIGN KEY (`policy_id`) REFERENCES `sanction_policy` (`policy_id`),
  CONSTRAINT `ck_consultation_close_reason` CHECK ((`close_reason` in (_utf8mb4'NORMAL',_utf8mb4'ABUSE_AUTO',_utf8mb4'ABUSE_MANUAL',_utf8mb4'QUEUE_CANCEL'))),
  CONSTRAINT `ck_consultation_closed_by` CHECK ((`closed_by` in (_utf8mb4'CUSTOMER',_utf8mb4'AGENT',_utf8mb4'SYSTEM'))),
  CONSTRAINT `ck_consultation_result` CHECK ((`result` in (_utf8mb4'RESOLVED',_utf8mb4'UNRESOLVED',_utf8mb4'TERMINATED'))),
  CONSTRAINT `ck_consultation_status` CHECK ((`status` in (_utf8mb4'AI_CHAT',_utf8mb4'QUEUED',_utf8mb4'IN_PROGRESS',_utf8mb4'CLOSED',_utf8mb4'TERMINATED')))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='상담';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `consultation` WRITE;
/*!40000 ALTER TABLE `consultation` DISABLE KEYS */;
INSERT INTO `consultation` VALUES (1,8,4,1,'TERMINATED','기타','주민센터에 낸 민원이 몇 주째 처리가 안 되고 있어요.','2026-09-10 13:00:00','2026-09-10 13:01:05','2026-09-10 13:02:35','2026-09-10 13:08:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(2,8,2,1,'TERMINATED','기타','구청에 신청한 보조금 지급이 계속 늦어지고 있어요.','2026-09-25 10:30:00','2026-09-25 10:31:05','2026-09-25 10:32:35','2026-09-25 10:38:33','AGENT','ABUSE_MANUAL','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 폭언으로 상담원 종료 (폭언 감지 1회)','TERMINATED'),(3,7,2,1,'TERMINATED','기타','도로 파손 신고를 했는데 보수가 안 되고 있어요.','2026-10-20 11:00:00','2026-10-20 11:01:05','2026-10-20 11:02:35','2026-10-20 11:08:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(4,10,3,1,'TERMINATED','기타','버스 정류장 안내 전광판이 일주일째 고장 나 있어요.','2026-11-02 15:00:00','2026-11-02 15:01:05','2026-11-02 15:02:35','2026-11-02 15:08:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(5,11,NULL,1,'CLOSED','상하수도','수도요금 납부 방법이 어떻게 됩니까?','2026-11-03 10:05:00',NULL,NULL,'2026-11-03 10:06:35','CUSTOMER','NORMAL','AI 응대 → 고객 종료','RESOLVED'),(6,8,3,1,'TERMINATED','기타','불법 주정차 신고했는데 단속이 안 되고 있어요.','2026-11-03 17:20:00','2026-11-03 17:21:05','2026-11-03 17:22:35','2026-11-03 17:28:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(7,9,4,1,'TERMINATED','기타','공원 가로등이 꺼져 있어서 밤에 너무 어두워요.','2026-11-04 12:00:00','2026-11-04 12:01:05','2026-11-04 12:02:35','2026-11-04 12:08:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(8,7,3,1,'TERMINATED','기타','쓰레기 무단투기 신고를 여러 번 했는데 그대로예요.','2026-11-05 16:00:00','2026-11-05 16:01:05','2026-11-05 16:02:35','2026-11-05 16:08:06','SYSTEM','ABUSE_AUTO','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)','TERMINATED'),(9,11,2,1,'CLOSED','기타','지난달 아파트 단지 전체가 단수됐는데 보상받을 방법이 있나요?','2026-11-06 14:20:00','2026-11-06 14:21:05','2026-11-06 14:22:35','2026-11-06 14:27:10','AGENT','NORMAL','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 상담원 종료','RESOLVED'),(10,10,2,1,'CLOSED','기타','여권 재발급 신청했는데 언제 나오는지 알 수 있나요?','2026-11-07 11:00:00','2026-11-07 11:01:05','2026-11-07 11:02:35','2026-11-07 11:09:03','AGENT','NORMAL','AI 응대 → 상담원 연결 요청 → 상담원 상담 → 상담원 종료 (폭언 감지 1회)','RESOLVED'),(11,11,NULL,1,'CLOSED','기타','동네 공영주차장 월 정기권은 어디서 신청하나요?','2026-11-09 09:10:00','2026-11-09 09:11:05',NULL,'2026-11-09 09:14:05','CUSTOMER','QUEUE_CANCEL','AI 응대 → 상담원 연결 요청 → 대기 중 취소','UNRESOLVED'),(12,10,NULL,1,'CLOSED','대중교통','버스 타고 지하철로 갈아탈 때 환승이 되나요?','2026-11-10 13:30:00',NULL,NULL,'2026-11-10 13:31:35','CUSTOMER','NORMAL','AI 응대 → 고객 종료','RESOLVED'),(13,10,NULL,1,'CLOSED','대중교통','대중교통 이용할 때 잃어버린 물건은 어디서 확인하나요?','2026-11-10 16:45:00',NULL,NULL,'2026-11-10 16:46:35','CUSTOMER','NORMAL','AI 응대 → 고객 종료','RESOLVED');
/*!40000 ALTER TABLE `consultation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `faq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faq` (
  `faq_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'FAQ ID',
  `category` varchar(50) NOT NULL COMMENT '문의 유형',
  `question` varchar(255) NOT NULL COMMENT '질문',
  `answer` text NOT NULL COMMENT '답변',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`faq_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FAQ';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `faq` WRITE;
/*!40000 ALTER TABLE `faq` DISABLE KEYS */;
INSERT INTO `faq` VALUES (1,'상하수도','수도요금 납부 방법이 어떻게 됩니까?','청구서, 자동납부, 입금전용계좌, 인터넷, ARS, 편의점, 스마트 납부, ATM으로 낼 수 있어요. 청구서 납부는 청구서를 가지고 한국은행을 뺀 전국 은행 창구나 공과금 수납기에서 평일 오전 9시~오후 4시에 하면 돼요. 편의점은 CU와 GS25에서 24시간 카드로 낼 수 있어요.','2026-11-11 16:48:36'),(2,'상하수도','이번 달 수도요금은 어디서 조회할 수 있습니까?','아리수 사이버고객센터(i121.seoul.go.kr)에서 회원가입 없이 조회할 수 있어요. 고지서에 적힌 고객번호가 필요하고, 모르면 관할 수도사업소 요금과에 문의하세요.','2026-11-11 16:48:36'),(3,'상하수도','이사 갈 때 수도요금 정산은 어떻게 합니까?','이사 가는 날 다산콜센터에 전화하면 정산 요금을 안내받을 수 있고, 계량기를 확인한 뒤 가상계좌로 내면 돼요. 나가는 사람은 자동납부, 이메일 청구서, 자가검침도 해지해야 해요. 들어오는 사람은 이전 거주자의 체납과 정산 여부를 확인하고 영수증을 주고받아 두면 분쟁을 막을 수 있어요.','2026-11-11 16:48:36'),(4,'상하수도','수도 명의 변경을 하려고 합니다.','아리수 사이버고객센터 온라인 민원으로 신청할 수 있어요. 사용자 명의변경은 신고 즉시 처리되고, 소유자 명의변경은 명의변경신고서와 등기부등본이나 건축물관리대장 같은 증빙서류가 필요해요. 담당은 관할 수도사업소예요.','2026-11-11 16:48:36'),(5,'상하수도','수도요금 자동납부는 어떻게 신청합니까?','계좌 자동납부는 다산콜센터나 수도사업소 전화, 수도사업소나 거래은행 방문, 상수도 누리집, 지로 누리집, 서울시 지방세 납부시스템에서 신청할 수 있어요. 전화 신청은 평일 오전 9시 10분~오후 6시에 돼요.','2026-11-11 16:48:36'),(6,'상하수도','수도 계량기가 고장 난 것 같습니다.','관할 수도사업소나 다산콜센터, 아리수 사이버고객센터 온라인 민원으로 교체를 신청하면 돼요. 현장조사 후 교체하고 법정 처리기간은 5일이에요. 고장 원인에 따라 계량기 대금과 설치비가 들 수 있고, 관리 부주의로 파손된 경우 수수료가 붙어요.','2026-11-11 16:48:36'),(7,'상하수도','수도가 얼어서 물이 안 나옵니다.','유리가 깨지지 않은 단순 동결이면 계량기 주변을 미지근한 물이나 헤어드라이어로 녹여 보세요. 그래도 안 되거나 유리가 깨진 동파라면 고지서의 고객번호를 가지고 관할 수도사업소에 접수하세요. 사용자 부주의가 아니면 수리비는 따로 청구되지 않아요.','2026-11-11 16:48:36'),(8,'상하수도','수질검사 신청은 어떻게 하면 됩니까?','다산콜센터나 아리수 사이버고객센터 민원신청으로 신청하면 담당자가 집을 방문해 잔류염소, 탁도, 철, 구리, pH 5가지를 검사해요. 일반적인 먹는물 수질검사는 인증된 민간 먹는물 수질검사기관에 맡기면 돼요.','2026-11-11 16:48:36'),(9,'상하수도','누수 때문에 수도요금이 너무 많이 나왔는데 어떻게 하나요?','누수감액을 신청할 수 있어요. 수리영수증이나 누수 전후 사진 같은 증빙을 가지고 관할 수도사업소에 방문하거나 팩스로 보내면 되고, 고지서를 받은 날부터 90일 이내에 신청해야 해요. 옥내 누수는 사용자가 직접 고쳐야 하고 수리비 지원은 없어요.','2026-11-11 16:48:36'),(10,'일반행정','지방세를 내려면 어떻게 해야 됩니까?','위택스(www.wetax.go.kr)나 스마트위택스 앱, 이용하는 은행의 누리집이나 앱, 은행 창구, ATM에서 낼 수 있어요. 위택스에서 이메일 같은 전자송달도 신청할 수 있고, 주민세·재산세·자동차세는 자동납부가 돼요.','2026-11-11 16:48:36'),(11,'일반행정','지방세를 납부하지 않으면 어떻게 되나요?','체납하면 독촉 후 재산이 압류될 수 있고, 신용불량 등록이나 고액체납자 명단 공개 같은 조치도 있어요. 위택스에서 바로 조회하고 납부할 수 있어요.','2026-11-11 16:48:36'),(12,'일반행정','청년수당을 받다가 취업을 하면 어떻게 하죠?','자격상실 신고를 바로 해야 하고, 창업해도 마찬가지예요. 신고하지 않으면 받은 돈의 전부나 일부를 반납해야 할 수 있어요. 상실신고일 기준으로 다음 회차까지는 지급돼요.','2026-11-11 16:48:36'),(13,'일반행정','청년수당은 한 번만 받을 수 있나요?','청년수당은 생애 1회만 받을 수 있고, 받는 중에는 청년특별구직지원금과 중복으로 받을 수 없어요. 주소가 바뀌면 청년포털에서 정보를 변경하세요. 국민연금 납부처럼 개인 재산을 쌓는 데는 쓸 수 없어요.','2026-11-11 16:48:36'),(14,'일반행정','미혼인데 아이를 낳았어요. 출생신고 가능한가요?','혼인 외 출생은 어머니가 신고하는 것이 원칙이고, 아버지가 친생자로 인지한 경우 아버지도 신고할 수 있어요. 어머니가 직접 갈 때는 출생신고서 원본, 출생증명서 원본, 신분증 원본이 필요해요.','2026-11-11 16:48:36'),(15,'일반행정','문화누리카드는 어디에서 쓸 수 있나요?','사용처는 문화누리카드 누리집의 가맹점 메뉴에서 확인할 수 있어요. 일반 버스와 지하철에는 쓸 수 없고 항공, 고속버스, 열차에는 쓸 수 있어요. 잔액도 누리집에서 확인할 수 있어요.','2026-11-11 16:48:36'),(16,'일반행정','문화누리카드 잔액이 다음 해로 이월되나요?','이월되지 않아요. 그해 12월 말까지만 쓸 수 있고 남은 금액은 자동으로 사라져요. 매년 다시 신청해 자격을 확인받은 뒤 충전해서 써야 하고, 대상 여부는 동주민센터나 누리집에서 확인할 수 있어요.','2026-11-11 16:48:36'),(17,'일반행정','경로우대 교통카드는 어디서 발급받나요?','주민등록 주소지가 서울이면 가까운 동주민센터 어디서나 단순무임카드를 발급받을 수 있고, 처음 발급은 본인이 직접 가야 해요. 만 65세 생일 2개월 전부터 신청할 수 있어요. 신용·체크 기능이 있는 카드는 은행에서 발급받아요.','2026-11-11 16:48:36'),(18,'일반행정','서울사랑상품권은 어디에서 쓸 수 있나요?','서울사랑상품권은 자치구별 모바일 상품권을 합친 이름이라, 구 상품권은 발행한 자치구 안의 제로페이 가맹점에서만 쓸 수 있어요. 대형마트에서는 쓸 수 없고, 가맹점은 제로페이 누리집에서 확인할 수 있어요.','2026-11-11 16:48:36'),(19,'일반행정','제로페이 가맹점인지 어떻게 알 수 있나요?','제로페이 앱의 가맹점 찾기에서 주변 가맹점을 확인할 수 있어요. 가맹점에서 앱의 QR코드나 바코드를 보여주면 결제되고, 할인되는 업체도 앱에서 확인할 수 있어요.','2026-11-11 16:48:36'),(20,'일반행정','따릉이는 어떻게 이용하나요?','서울자전거 앱에서 1시간권이나 2시간권 같은 이용권을 사고 서울자전거 대여소에서 빌리면 돼요. 반납은 가까운 따릉이 대여소 아무 곳에나 하면 되고, 기본 이용시간을 넘기면 초과 요금이 붙어요.','2026-11-11 16:48:36'),(21,'일반행정','집 근처에서 고양이가 새끼를 낳았는데 위험해 보여요.','정확한 위치를 알려주면 담당 직원이 나가서 협력 동물병원으로 인계해요. 길고양이 중성화는 비용 일부를 지원하는 사업이 있고, 나머지는 구조자나 보호자가 부담해요.','2026-11-11 16:48:36'),(22,'대중교통','대중교통 이용할 때 잃어버린 물건은 어디서 확인하나요?','서울시 누리집의 \'대중교통 분실물 찾기\'에서 확인할 수 있어요. 버스 회사에 직접 전화해도 되고, 탄 시간과 노선을 대략이라도 알면 찾는 데 도움이 돼요.','2026-11-11 16:48:36'),(23,'대중교통','택시에 물건을 두고 내렸어요.','호출 앱으로 탔다면 앱의 이용내역에서 차량번호를 확인하세요. 탑승 후 3일 이내면 기사와 바로 통화할 수 있고, 그 뒤에는 해당 고객센터에 택시 정보로 분실물 신고를 하면 돼요.','2026-11-11 16:48:36'),(24,'대중교통','교통카드가 고장 났는데 남은 잔액은 어떻게 하죠?','고장 난 카드를 봉투에 넣고 고장 사유와 사용 기간을 적어 카드를 발행한 회사(예: 티머니)로 보내면 돼요. 확인 절차를 거쳐 잔액은 전액 통장으로 환불되고, 새 카드를 받을지도 고를 수 있어요.','2026-11-11 16:48:36'),(25,'대중교통','지하철에 자전거를 가지고 탈 수 있나요?','일반 자전거는 9호선을 뺀 노선에서 주말과 공휴일에 맨 앞칸과 맨 뒤칸으로 탈 수 있어요. 접이식 자전거는 평일에도 전 노선에서 가지고 탈 수 있어요. 평일 출퇴근 시간(오전 7~9시, 오후 5~8시)에는 제한될 수 있어요.','2026-11-11 16:48:36'),(26,'대중교통','음료를 들고 버스를 탈 수 있나요?','일회용 컵에 담긴 음료나 음식물은 버스에 가지고 탈 수 없어요. 텀블러라도 뚜껑이 닫히지 않으면 가지고 탈 수 없어요.','2026-11-11 16:48:36'),(27,'대중교통','불친절한 택시 기사를 신고하려고 합니다.','차량번호, 탄 곳과 내린 곳, 탄 시간을 알려주면 접수할 수 있어요. 본인 확인을 위해 이름, 연락처, 주소가 필요하고 개인정보는 보호돼요. 신고 내용은 교통 담당 부서로 넘어가고 처리 결과는 문자로 알려줘요.','2026-11-11 16:48:36'),(28,'대중교통','KTX가 지연됐는데 보상받을 수 있나요?','지연 시간에 따라 보상받을 수 있어요. 현금이나 할인증으로 받을 수 있고, 지연된 날부터 1년 이내에 모든 역에서 받을 수 있어요.','2026-11-11 16:48:36'),(29,'대중교통','버스 타고 지하철로 갈아탈 때 환승이 되나요?','환승할인은 최대 4회까지 돼요. 내린 뒤 30분 안에(오후 9시~오전 7시는 1시간 안에) 타야 하고, 같은 번호 버스를 다시 타거나 지하철에서 지하철로 다시 타는 건 환승이 안 돼요.','2026-11-11 16:48:36'),(30,'대중교통','행사 때문에 도로가 통제되면 우회 노선은 어디서 확인하나요?','통제 구간과 버스 임시 우회 노선은 서울교통정보센터 누리집에서 확인할 수 있어요. 근처 지하철역도 통제될 수 있고, 시간은 현장 상황에 따라 바뀔 수 있어요.','2026-11-11 16:48:36');
/*!40000 ALTER TABLE `faq` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `faq_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faq_keyword` (
  `keyword_id` bigint NOT NULL AUTO_INCREMENT COMMENT '키워드 ID',
  `faq_id` bigint NOT NULL COMMENT 'FAQ ID',
  `keyword` varchar(50) NOT NULL COMMENT '매칭 키워드',
  PRIMARY KEY (`keyword_id`),
  UNIQUE KEY `uk_faq_keyword` (`faq_id`,`keyword`),
  KEY `ix_faq_keyword_keyword` (`keyword`),
  CONSTRAINT `fk_faq_keyword_faq` FOREIGN KEY (`faq_id`) REFERENCES `faq` (`faq_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=194 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FAQ 키워드';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `faq_keyword` WRITE;
/*!40000 ALTER TABLE `faq_keyword` DISABLE KEYS */;
INSERT INTO `faq_keyword` VALUES (7,1,'고지서납부'),(2,1,'납부방법'),(1,1,'수도세'),(3,1,'요금납부'),(6,1,'청구서'),(5,1,'편의점'),(4,1,'편의점납부'),(13,2,'고객번호'),(15,2,'납부내역'),(12,2,'사이버고객센터'),(9,2,'수도요금조회'),(11,2,'아리수'),(14,2,'얼마나왔'),(8,2,'요금조회'),(10,2,'조회'),(16,3,'이사'),(22,3,'이사가'),(18,3,'이사정산'),(21,3,'전입'),(20,3,'전출'),(17,3,'정산'),(19,3,'중간정산'),(24,4,'명의'),(23,4,'명의변경'),(27,4,'명의이전'),(26,4,'사용자변경'),(25,4,'소유자변경'),(32,5,'계좌이체신청'),(28,5,'자동납부'),(30,5,'자동납부신청'),(29,5,'자동이체'),(31,5,'카드자동납부'),(33,6,'계량기'),(34,6,'계량기고장'),(35,6,'계량기교체'),(36,6,'계량기파손'),(37,6,'침이'),(41,7,'계량기가얼'),(45,7,'계량기동파'),(48,7,'단수'),(43,7,'동결'),(42,7,'동파'),(44,7,'동파예방'),(47,7,'물안나'),(46,7,'물이안나'),(38,7,'얼어'),(40,7,'얼어서'),(39,7,'얼었'),(52,8,'녹물'),(55,8,'물색'),(53,8,'소독냄새'),(51,8,'수돗물'),(50,8,'수질'),(49,8,'수질검사'),(54,8,'유충'),(57,9,'감액'),(56,9,'누수'),(58,9,'누수감액'),(59,9,'많이나왔'),(61,9,'물이새'),(60,9,'요금폭탄'),(68,10,'세금납부'),(63,10,'위택스'),(66,10,'자동차세'),(65,10,'재산세'),(67,10,'주민세'),(62,10,'지방세'),(64,10,'지방세납부'),(72,11,'납부하지않'),(73,11,'내지않'),(75,11,'독촉'),(70,11,'미납'),(78,11,'세금체납'),(71,11,'안내면'),(74,11,'압류'),(76,11,'연체'),(77,11,'지방세체납'),(69,11,'체납'),(81,12,'상실신고'),(82,12,'자격상실'),(83,12,'창업'),(79,12,'청년수당취업'),(80,12,'취업'),(89,13,'몇번'),(86,13,'생애'),(87,13,'중복'),(88,13,'중복수령'),(84,13,'청년수당'),(85,13,'한번만'),(92,14,'미혼'),(95,14,'사실혼'),(93,14,'아이를낳'),(91,14,'출생'),(90,14,'출생신고'),(94,14,'혼인외'),(98,15,'누리카드'),(99,15,'누리카드사용처'),(100,15,'누리카드잔액'),(97,15,'문화누리'),(96,15,'문화누리카드'),(103,16,'남은금액'),(104,16,'다음해'),(102,16,'소멸'),(101,16,'이월'),(105,16,'재신청'),(106,16,'재충전'),(112,17,'65세'),(107,17,'경로우대'),(108,17,'경로우대카드'),(110,17,'무임카드'),(109,17,'어르신교통카드'),(111,17,'우대용'),(117,18,'구상품권'),(114,18,'사랑상품권'),(115,18,'상품권'),(113,18,'서울사랑상품권'),(116,18,'지역화폐'),(119,19,'가맹점찾기'),(118,19,'제로페이'),(120,19,'제로페이가맹점'),(121,19,'제로페이결제'),(123,20,'공공자전거'),(122,20,'따릉이'),(125,20,'서울자전거'),(124,20,'자전거대여'),(126,20,'자전거빌리'),(127,21,'고양이'),(128,21,'길고양이'),(132,21,'동물구조'),(130,21,'새끼고양이'),(131,21,'유기견'),(129,21,'유기동물'),(133,21,'중성화'),(137,22,'놓고내'),(136,22,'두고내'),(139,22,'분실'),(134,22,'분실물'),(138,22,'유실물'),(135,22,'잃어버'),(144,23,'놓고내'),(143,23,'두고내'),(146,23,'분실'),(141,23,'분실물'),(145,23,'유실물'),(142,23,'잃어버'),(140,23,'택시'),(147,24,'교통카드'),(149,24,'교통카드고장'),(152,24,'인식이안'),(150,24,'잔액'),(148,24,'카드고장'),(151,24,'티머니'),(153,25,'자전거'),(156,25,'자전거탑승'),(155,25,'자전거휴대'),(154,25,'접이식'),(157,25,'지하철자전거'),(158,26,'음료'),(164,26,'음료수'),(163,26,'음식물'),(161,26,'일회용컵'),(159,26,'커피'),(162,26,'텀블러'),(160,26,'테이크아웃'),(166,27,'기사신고'),(172,27,'난폭운전'),(170,27,'버스기사'),(168,27,'버스신고'),(165,27,'불친절'),(173,27,'불편신고'),(171,27,'승차거부'),(175,27,'신고'),(174,27,'신고방법'),(169,27,'택시기사'),(167,27,'택시신고'),(180,28,'KTX'),(179,28,'기차지연'),(182,28,'늦게도착'),(177,28,'보상'),(178,28,'연착'),(176,28,'지연'),(184,29,'갈아타'),(187,29,'무료환승'),(183,29,'환승'),(186,29,'환승시간'),(185,29,'환승할인'),(191,30,'교통통제'),(189,30,'우회'),(192,30,'우회노선'),(193,30,'축제'),(188,30,'통제'),(190,30,'통행제한');
/*!40000 ALTER TABLE `faq_keyword` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
  `login_id` varchar(20) NOT NULL COMMENT '로그인 아이디',
  `password` varchar(255) NOT NULL COMMENT '비밀번호',
  `name` varchar(50) NOT NULL COMMENT '이름',
  `role` varchar(20) NOT NULL COMMENT '역할(고객/상담원/관리자)',
  `status` varchar(20) NOT NULL COMMENT '회원 상태(활성/비활성)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_login_id` (`login_id`),
  CONSTRAINT `ck_member_role` CHECK ((`role` in (_utf8mb4'CUSTOMER',_utf8mb4'AGENT',_utf8mb4'ADMIN'))),
  CONSTRAINT `ck_member_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (1,'admin01','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','관리자','ADMIN','ACTIVE','2026-09-01 09:00:00'),(2,'agent01','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','심소민','AGENT','ACTIVE','2026-09-01 09:00:00'),(3,'agent02','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','유능한','AGENT','ACTIVE','2026-09-01 09:00:00'),(4,'agent03','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','장건웅','AGENT','ACTIVE','2026-09-01 09:00:00'),(5,'agent04','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','한수민','AGENT','ACTIVE','2026-09-01 09:00:00'),(6,'agent05','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','정혜영','AGENT','ACTIVE','2026-09-01 09:00:00'),(7,'user03','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','이영희','CUSTOMER','ACTIVE','2026-09-04 12:00:00'),(8,'user04','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','박철수','CUSTOMER','ACTIVE','2026-09-05 13:00:00'),(9,'user05','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','최지은','CUSTOMER','ACTIVE','2026-09-06 14:00:00'),(10,'user06','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','정우성','CUSTOMER','ACTIVE','2026-09-07 15:00:00'),(11,'user07','$2a$10$m7bA5IzalkWD0iTndk7hC.a7Vfczy3qqQgwzu3sJOJ9OV1i0jnjsC','한소희','CUSTOMER','ACTIVE','2026-09-08 16:00:00');
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '메시지 ID',
  `consultation_id` bigint NOT NULL COMMENT '상담 ID',
  `sender_type` varchar(20) NOT NULL COMMENT '발신자 유형',
  `sender_id` bigint DEFAULT NULL COMMENT '발신 회원 ID',
  `content` text NOT NULL COMMENT '메시지 내용(마스킹)',
  `abuse_status` varchar(20) DEFAULT NULL COMMENT '폭언 판정(정상/폭언)',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '발송 일시',
  PRIMARY KEY (`message_id`),
  KEY `fk_message_sender` (`sender_id`),
  KEY `ix_message_consultation_time` (`consultation_id`,`created_at`),
  CONSTRAINT `fk_message_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation` (`consultation_id`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `ck_message_abuse_status` CHECK ((`abuse_status` in (_utf8mb4'NORMAL',_utf8mb4'ABUSIVE'))),
  CONSTRAINT `ck_message_sender_type` CHECK ((`sender_type` in (_utf8mb4'CUSTOMER',_utf8mb4'AGENT',_utf8mb4'AI',_utf8mb4'SYSTEM')))
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='메시지';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
INSERT INTO `message` VALUES (1,1,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-09-10 13:00:00.037'),(2,1,'CUSTOMER',8,'주민센터에 낸 민원이 몇 주째 처리가 안 되고 있어요.',NULL,'2026-09-10 13:00:20.074'),(3,1,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-09-10 13:00:25.111'),(4,1,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-09-10 13:02:35.148'),(5,1,'AGENT',4,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-09-10 13:02:50.185'),(6,1,'CUSTOMER',8,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-09-10 13:03:50.222'),(7,1,'AGENT',4,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-09-10 13:05:00.259'),(8,1,'CUSTOMER',8,'*** 아직도 처리 안 됐어요?','ABUSIVE','2026-09-10 13:06:10.296'),(9,1,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-09-10 13:06:13.333'),(10,1,'AGENT',4,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-09-10 13:07:03.370'),(11,1,'CUSTOMER',8,'*** 같은 답변 좀 그만하세요.','ABUSIVE','2026-09-10 13:08:03.407'),(12,1,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-09-10 13:08:06.444'),(13,2,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-09-25 10:30:00.481'),(14,2,'CUSTOMER',8,'구청에 신청한 보조금 지급이 계속 늦어지고 있어요.',NULL,'2026-09-25 10:30:20.518'),(15,2,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-09-25 10:30:25.555'),(16,2,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-09-25 10:32:35.592'),(17,2,'AGENT',2,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-09-25 10:32:50.629'),(18,2,'CUSTOMER',8,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-09-25 10:33:50.666'),(19,2,'AGENT',2,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-09-25 10:35:00.703'),(20,2,'CUSTOMER',8,'***들 일 똑바로 안 해요?','ABUSIVE','2026-09-25 10:36:10.740'),(21,2,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-09-25 10:36:13.777'),(22,2,'AGENT',2,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-09-25 10:37:03.814'),(23,2,'SYSTEM',NULL,'상담원이 부적절한 표현을 사유로 상담을 종료했습니다.',NULL,'2026-09-25 10:38:33.851'),(24,3,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-10-20 11:00:00.888'),(25,3,'CUSTOMER',7,'도로 파손 신고를 했는데 보수가 안 되고 있어요.',NULL,'2026-10-20 11:00:20.925'),(26,3,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-10-20 11:00:25.962'),(27,3,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-10-20 11:02:35.999'),(28,3,'AGENT',2,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-10-20 11:02:50.036'),(29,3,'CUSTOMER',7,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-10-20 11:03:50.073'),(30,3,'AGENT',2,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-10-20 11:05:00.110'),(31,3,'CUSTOMER',7,'***하지 말고 빨리 처리해요.','ABUSIVE','2026-10-20 11:06:10.147'),(32,3,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-10-20 11:06:13.184'),(33,3,'AGENT',2,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-10-20 11:07:03.221'),(34,3,'CUSTOMER',7,'*** 답답하네 진짜.','ABUSIVE','2026-10-20 11:08:03.258'),(35,3,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-10-20 11:08:06.295'),(36,4,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-02 15:00:00.332'),(37,4,'CUSTOMER',10,'버스 정류장 안내 전광판이 일주일째 고장 나 있어요.',NULL,'2026-11-02 15:00:20.369'),(38,4,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-02 15:00:25.406'),(39,4,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-02 15:02:35.443'),(40,4,'AGENT',3,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-02 15:02:50.480'),(41,4,'CUSTOMER',10,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-11-02 15:03:50.517'),(42,4,'AGENT',3,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-11-02 15:05:00.554'),(43,4,'CUSTOMER',10,'*** 몇 번을 말해야 돼요?','ABUSIVE','2026-11-02 15:06:10.591'),(44,4,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-11-02 15:06:13.628'),(45,4,'AGENT',3,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-11-02 15:07:03.665'),(46,4,'CUSTOMER',10,'*** 아직도 처리 안 됐어요?','ABUSIVE','2026-11-02 15:08:03.702'),(47,4,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-11-02 15:08:06.739'),(48,5,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-03 10:05:00.776'),(49,5,'CUSTOMER',11,'수도요금 납부 방법이 어떻게 됩니까?',NULL,'2026-11-03 10:05:20.813'),(50,5,'AI',NULL,'청구서, 자동납부, 입금전용계좌, 인터넷, ARS, 편의점, 스마트 납부, ATM으로 낼 수 있어요. 청구서 납부는 청구서를 가지고 한국은행을 뺀 전국 은행 창구나 공과금 수납기에서 평일 오전 9시~오후 4시에 하면 돼요. 편의점은 CU와 GS25에서 24시간 카드로 낼 수 있어요.',NULL,'2026-11-03 10:05:25.850'),(51,5,'CUSTOMER',11,'네 이해했어요. 감사합니다.',NULL,'2026-11-03 10:06:25.887'),(52,6,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-03 17:20:00.924'),(53,6,'CUSTOMER',8,'불법 주정차 신고했는데 단속이 안 되고 있어요.',NULL,'2026-11-03 17:20:20.961'),(54,6,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-03 17:20:25.998'),(55,6,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-03 17:22:35.035'),(56,6,'AGENT',3,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-03 17:22:50.072'),(57,6,'CUSTOMER',8,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-11-03 17:23:50.109'),(58,6,'AGENT',3,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-11-03 17:25:00.146'),(59,6,'CUSTOMER',8,'*** 같은 답변 좀 그만하세요.','ABUSIVE','2026-11-03 17:26:10.183'),(60,6,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-11-03 17:26:13.220'),(61,6,'AGENT',3,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-11-03 17:27:03.257'),(62,6,'CUSTOMER',8,'*** 답답하네 진짜.','ABUSIVE','2026-11-03 17:28:03.294'),(63,6,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-11-03 17:28:06.331'),(64,7,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-04 12:00:00.368'),(65,7,'CUSTOMER',9,'공원 가로등이 꺼져 있어서 밤에 너무 어두워요.',NULL,'2026-11-04 12:00:20.405'),(66,7,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-04 12:00:25.442'),(67,7,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-04 12:02:35.479'),(68,7,'AGENT',4,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-04 12:02:50.516'),(69,7,'CUSTOMER',9,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-11-04 12:03:50.553'),(70,7,'AGENT',4,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-11-04 12:05:00.590'),(71,7,'CUSTOMER',9,'***하지 말고 빨리 처리해요.','ABUSIVE','2026-11-04 12:06:10.627'),(72,7,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-11-04 12:06:13.664'),(73,7,'AGENT',4,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-11-04 12:07:03.701'),(74,7,'CUSTOMER',9,'*** 몇 번을 말해야 돼요?','ABUSIVE','2026-11-04 12:08:03.738'),(75,7,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-11-04 12:08:06.775'),(76,8,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-05 16:00:00.812'),(77,8,'CUSTOMER',7,'쓰레기 무단투기 신고를 여러 번 했는데 그대로예요.',NULL,'2026-11-05 16:00:20.849'),(78,8,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-05 16:00:25.886'),(79,8,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-05 16:02:35.923'),(80,8,'AGENT',3,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-05 16:02:50.960'),(81,8,'CUSTOMER',7,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-11-05 16:03:50.997'),(82,8,'AGENT',3,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-11-05 16:05:00.034'),(83,8,'CUSTOMER',7,'*** 답답하네 진짜.','ABUSIVE','2026-11-05 16:06:10.071'),(84,8,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-11-05 16:06:13.108'),(85,8,'AGENT',3,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-11-05 16:07:03.145'),(86,8,'CUSTOMER',7,'*** 아직도 처리 안 됐어요?','ABUSIVE','2026-11-05 16:08:03.182'),(87,8,'SYSTEM',NULL,'같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라 상담이 자동 종료되었습니다.',NULL,'2026-11-05 16:08:06.219'),(88,9,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-06 14:20:00.256'),(89,9,'CUSTOMER',11,'지난달 아파트 단지 전체가 단수됐는데 보상받을 방법이 있나요?',NULL,'2026-11-06 14:20:20.293'),(90,9,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-06 14:20:25.330'),(91,9,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-06 14:22:35.367'),(92,9,'AGENT',2,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-06 14:22:50.404'),(93,9,'CUSTOMER',11,'네, 사전 안내 없이 반나절 동안 물이 안 나왔어요.','NORMAL','2026-11-06 14:23:50.441'),(94,9,'AGENT',2,'관할 수도사업소에 피해 접수를 하시면 검토 후 안내해 드립니다. 접수 방법을 문자로 보내드릴게요.',NULL,'2026-11-06 14:25:00.478'),(95,9,'AGENT',2,'안내드린 방법으로 처리 부탁드립니다. 더 궁금하신 점 있으실까요?',NULL,'2026-11-06 14:26:10.515'),(96,9,'CUSTOMER',11,'아니요, 해결됐어요. 감사합니다.','NORMAL','2026-11-06 14:26:50.552'),(97,10,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-07 11:00:00.589'),(98,10,'CUSTOMER',10,'여권 재발급 신청했는데 언제 나오는지 알 수 있나요?',NULL,'2026-11-07 11:00:20.626'),(99,10,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-07 11:00:25.663'),(100,10,'SYSTEM',NULL,'상담원이 연결되었습니다. 이전 AI 상담 내용이 전달되었습니다.',NULL,'2026-11-07 11:02:35.700'),(101,10,'AGENT',2,'안녕하세요, 상담원입니다. 남겨주신 문의 확인했습니다. 조금 더 자세히 말씀해 주시겠어요?',NULL,'2026-11-07 11:02:50.737'),(102,10,'CUSTOMER',10,'네, 오늘 오전에 신청했는데 아직 연락이 없어서요.','NORMAL','2026-11-07 11:03:50.774'),(103,10,'AGENT',2,'확인해 보니 접수는 정상적으로 되어 있습니다. 담당 부서에서 순차적으로 처리 중입니다.',NULL,'2026-11-07 11:05:00.811'),(104,10,'CUSTOMER',10,'*** 답답하네 진짜.','ABUSIVE','2026-11-07 11:06:10.848'),(105,10,'SYSTEM',NULL,'[시스템 경고] 부적절한 표현이 감지되었습니다. 상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서 다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.',NULL,'2026-11-07 11:06:13.885'),(106,10,'AGENT',2,'고객님, 원활한 상담을 위해 표현을 순화해 주시면 감사하겠습니다.',NULL,'2026-11-07 11:07:03.922'),(107,10,'AGENT',2,'안내드린 방법으로 처리 부탁드립니다. 더 궁금하신 점 있으실까요?',NULL,'2026-11-07 11:08:03.959'),(108,10,'CUSTOMER',10,'아니요, 해결됐어요. 감사합니다.','NORMAL','2026-11-07 11:08:43.996'),(109,11,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-09 09:10:00.033'),(110,11,'CUSTOMER',11,'동네 공영주차장 월 정기권은 어디서 신청하나요?',NULL,'2026-11-09 09:10:20.070'),(111,11,'AI',NULL,'죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접 상담하시겠어요?',NULL,'2026-11-09 09:10:25.107'),(112,12,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-10 13:30:00.144'),(113,12,'CUSTOMER',10,'버스 타고 지하철로 갈아탈 때 환승이 되나요?',NULL,'2026-11-10 13:30:20.181'),(114,12,'AI',NULL,'환승할인은 최대 4회까지 돼요. 내린 뒤 30분 안에(오후 9시~오전 7시는 1시간 안에) 타야 하고, 같은 번호 버스를 다시 타거나 지하철에서 지하철로 다시 타는 건 환승이 안 돼요.',NULL,'2026-11-10 13:30:25.218'),(115,12,'CUSTOMER',10,'네 이해했어요. 감사합니다.',NULL,'2026-11-10 13:31:25.255'),(116,13,'AI',NULL,'안녕하세요, AI 상담입니다. 궁금한 내용을 입력하시거나 자주 묻는 질문을 선택해 주세요.',NULL,'2026-11-10 16:45:00.292'),(117,13,'CUSTOMER',10,'대중교통 이용할 때 잃어버린 물건은 어디서 확인하나요?',NULL,'2026-11-10 16:45:20.329'),(118,13,'AI',NULL,'서울시 누리집의 \'대중교통 분실물 찾기\'에서 확인할 수 있어요. 버스 회사에 직접 전화해도 되고, 탄 시간과 노선을 대략이라도 알면 찾는 데 도움이 돼요.',NULL,'2026-11-10 16:45:25.366'),(119,13,'CUSTOMER',10,'네 이해했어요. 감사합니다.',NULL,'2026-11-10 16:46:25.403');
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sanction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sanction` (
  `sanction_id` bigint NOT NULL AUTO_INCREMENT COMMENT '이용정지 ID',
  `violation_id` bigint NOT NULL COMMENT '위반 ID',
  `customer_id` bigint NOT NULL COMMENT '고객 ID',
  `suspend_days` int NOT NULL COMMENT '정지 일수',
  `start_at` datetime NOT NULL COMMENT '정지 시작 일시',
  `end_at` datetime NOT NULL COMMENT '해제 예정 일시',
  `released_at` datetime DEFAULT NULL COMMENT '수동 해제 일시',
  `released_by` bigint DEFAULT NULL COMMENT '해제한 관리자 ID',
  `release_reason` varchar(255) DEFAULT NULL COMMENT '해제 사유',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`sanction_id`),
  UNIQUE KEY `uk_sanction_violation_id` (`violation_id`),
  KEY `fk_sanction_released_by` (`released_by`),
  KEY `ix_sanction_customer_end` (`customer_id`,`end_at`),
  CONSTRAINT `fk_sanction_customer` FOREIGN KEY (`customer_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_sanction_released_by` FOREIGN KEY (`released_by`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_sanction_violation` FOREIGN KEY (`violation_id`) REFERENCES `violation` (`violation_id`),
  CONSTRAINT `ck_sanction_days` CHECK ((`suspend_days` between 1 and 365)),
  CONSTRAINT `ck_sanction_period` CHECK ((`end_at` > `start_at`))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='이용정지';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sanction` WRITE;
/*!40000 ALTER TABLE `sanction` DISABLE KEYS */;
INSERT INTO `sanction` VALUES (1,1,8,7,'2026-09-10 13:08:06','2026-09-17 13:08:06',NULL,NULL,NULL,'2026-09-10 13:08:06'),(2,2,8,30,'2026-09-25 10:38:33','2026-10-25 10:38:33',NULL,NULL,NULL,'2026-09-25 10:38:33'),(3,3,7,7,'2026-10-20 11:08:06','2026-10-27 11:08:06',NULL,NULL,NULL,'2026-10-20 11:08:06'),(4,4,10,7,'2026-11-02 15:08:06','2026-11-09 15:08:06',NULL,NULL,NULL,'2026-11-02 15:08:06'),(5,5,8,90,'2026-11-03 17:28:06','2027-02-01 17:28:06',NULL,NULL,NULL,'2026-11-03 17:28:06'),(6,6,9,7,'2026-11-04 12:08:06','2026-11-11 12:08:06','2026-11-06 10:00:00',1,'고객 이의 제기 검토 결과 오탐으로 확인되어 해제','2026-11-04 12:08:06'),(7,7,7,30,'2026-11-05 16:08:06','2026-12-05 16:08:06',NULL,NULL,NULL,'2026-11-05 16:08:06');
/*!40000 ALTER TABLE `sanction` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sanction_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sanction_policy` (
  `policy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '정책 ID',
  `warn_threshold` int NOT NULL COMMENT '경고 발송 기준(N회째)',
  `close_threshold` int NOT NULL COMMENT '자동 종료 기준(N회)',
  `first_days` int NOT NULL COMMENT '누적 1회 정지 일수',
  `second_days` int NOT NULL COMMENT '누적 2회 정지 일수',
  `third_days` int NOT NULL COMMENT '누적 3회 이상 정지 일수',
  `change_note` varchar(255) DEFAULT NULL COMMENT '변경 내용',
  `created_by` bigint DEFAULT NULL COMMENT '저장한 관리자 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '저장 일시',
  PRIMARY KEY (`policy_id`),
  KEY `fk_sanction_policy_created_by` (`created_by`),
  CONSTRAINT `fk_sanction_policy_created_by` FOREIGN KEY (`created_by`) REFERENCES `member` (`member_id`),
  CONSTRAINT `ck_policy_close` CHECK ((`close_threshold` between 1 and 5)),
  CONSTRAINT `ck_policy_days` CHECK (((`first_days` between 1 and 365) and (`second_days` between 1 and 365) and (`third_days` between 1 and 365))),
  CONSTRAINT `ck_policy_order` CHECK ((`close_threshold` > `warn_threshold`)),
  CONSTRAINT `ck_policy_warn` CHECK ((`warn_threshold` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='제재 정책';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sanction_policy` WRITE;
/*!40000 ALTER TABLE `sanction_policy` DISABLE KEYS */;
INSERT INTO `sanction_policy` VALUES (1,1,2,7,30,90,'기본 정책 등록',NULL,'2026-09-01 00:00:00');
/*!40000 ALTER TABLE `sanction_policy` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `violation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `violation` (
  `violation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '위반 ID',
  `customer_id` bigint NOT NULL COMMENT '고객 ID',
  `consultation_id` bigint NOT NULL COMMENT '상담 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '위반 일시',
  PRIMARY KEY (`violation_id`),
  UNIQUE KEY `uk_violation_consultation_id` (`consultation_id`),
  KEY `fk_violation_customer` (`customer_id`),
  CONSTRAINT `fk_violation_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation` (`consultation_id`),
  CONSTRAINT `fk_violation_customer` FOREIGN KEY (`customer_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='위반 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `violation` WRITE;
/*!40000 ALTER TABLE `violation` DISABLE KEYS */;
INSERT INTO `violation` VALUES (1,8,1,'2026-09-10 13:08:06'),(2,8,2,'2026-09-25 10:38:33'),(3,7,3,'2026-10-20 11:08:06'),(4,10,4,'2026-11-02 15:08:06'),(5,8,6,'2026-11-03 17:28:06'),(6,9,7,'2026-11-04 12:08:06'),(7,7,8,'2026-11-05 16:08:06');
/*!40000 ALTER TABLE `violation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `warning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warning` (
  `warning_id` bigint NOT NULL AUTO_INCREMENT COMMENT '경고 ID',
  `consultation_id` bigint NOT NULL COMMENT '상담 ID',
  `abuse_log_id` bigint DEFAULT NULL COMMENT '폭언 기록 ID',
  `warning_type` varchar(20) NOT NULL COMMENT '경고 유형(자동/수동)',
  `issued_by` bigint DEFAULT NULL COMMENT '경고한 상담원 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '경고 일시',
  PRIMARY KEY (`warning_id`),
  UNIQUE KEY `uk_warning_abuse_log_id` (`abuse_log_id`),
  KEY `fk_warning_consultation` (`consultation_id`),
  KEY `fk_warning_issued_by` (`issued_by`),
  CONSTRAINT `fk_warning_abuse_log` FOREIGN KEY (`abuse_log_id`) REFERENCES `abuse_log` (`abuse_log_id`),
  CONSTRAINT `fk_warning_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation` (`consultation_id`),
  CONSTRAINT `fk_warning_issued_by` FOREIGN KEY (`issued_by`) REFERENCES `member` (`member_id`),
  CONSTRAINT `ck_warning_type` CHECK ((`warning_type` in (_utf8mb4'AUTO',_utf8mb4'MANUAL')))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='경고';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `warning` WRITE;
/*!40000 ALTER TABLE `warning` DISABLE KEYS */;
INSERT INTO `warning` VALUES (1,1,1,'AUTO',NULL,'2026-09-10 13:06:10'),(2,1,2,'AUTO',NULL,'2026-09-10 13:08:03'),(3,2,3,'AUTO',NULL,'2026-09-25 10:36:10'),(4,3,4,'AUTO',NULL,'2026-10-20 11:06:10'),(5,3,5,'AUTO',NULL,'2026-10-20 11:08:03'),(6,4,6,'AUTO',NULL,'2026-11-02 15:06:10'),(7,4,7,'AUTO',NULL,'2026-11-02 15:08:03'),(8,6,8,'AUTO',NULL,'2026-11-03 17:26:10'),(9,6,9,'AUTO',NULL,'2026-11-03 17:28:03'),(10,7,10,'AUTO',NULL,'2026-11-04 12:06:10'),(11,7,11,'AUTO',NULL,'2026-11-04 12:08:03'),(12,8,12,'AUTO',NULL,'2026-11-05 16:06:10'),(13,8,13,'AUTO',NULL,'2026-11-05 16:08:03'),(14,10,14,'AUTO',NULL,'2026-11-07 11:06:10');
/*!40000 ALTER TABLE `warning` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

