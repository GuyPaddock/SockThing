import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import com.redbottledesign.bitcoin.pool.drupal.Round;
import com.redbottledesign.bitcoin.pool.drupal.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.WittyRemark;
import com.redbottledesign.bitcoin.pool.drupal.WorkShare;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WittyRemarkRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class GetWorkers
{
  protected static final String DRUPAL_USER_NAME = "Pool manager daemon";
  protected static final String DRUPAL_PASSWORD  = "53a4ZlSlbnOcaaiYBN3Vh3M0H347adnFbFvAnqls5RSgq4bstzchxtU3BfiBongq";

  public static void main(String[] args)
  throws URISyntaxException, IOException, DrupalHttpException, ParseException
  {
    // bxhaYDnrAVbAraeBPMIRhutcfDKIanv3AnIH7Skb83Jzci6yuUbsSmIuhSwLAGhn
//    Gson    gson            = DrupalGsonFactory.getInstance().getGson();
//    Reader  data            = new InputStreamReader(GetWorkers.class.getResourceAsStream("nodes.json"), "UTF-8");
//    Type    resultListType  = new TypeToken<JsonEntityResultList<WittyRemark>>(){}.getType();
//
//    // Parse JSON to Java
//    JsonEntityResultList<WittyRemark> nodes = gson.fromJson(data, resultListType);
//
//    for (WittyRemark remark : nodes.getResults())
//    {
//      System.out.println(remark);
//    }
//
//    System.out.println(gson.toJson(nodes));
    // FIXME: Why do we have to specify the port #?
    SessionManager drupalSessionManager =
      new SessionManager(new URI("https://www.theredpool.com:443"), DRUPAL_USER_NAME, DRUPAL_PASSWORD);

//    System.out.println(drupalSessionManager.getSessionToken());
//
    WittyRemarkRequestor requestor = new WittyRemarkRequestor(drupalSessionManager);
    WittyRemark          node      = requestor.requestNodeByNid(11);
//
    System.out.println(node);
//    System.out.println(DrupalGsonFactory.getInstance().createGson().toJson(node, Node.class));
//
//    List<Node> nodes = requestor.requestNodesByType("faq");
//
//    System.out.println(Arrays.toString(nodes.toArray()));

//    node.setTitle(node.getTitle() + " CHANGED");
//
//    requestor.updateNode(node);

//    WittyRemark remark = new WittyRemark();
//
    UserRequestor userRequestor       = new UserRequestor(drupalSessionManager);
    User          poolManagementUser  = userRequestor.requestUserByUid(14);
//
//    System.out.println(poolManagementUser);
//
//    remark.setTitle("This is a fake remark.");
//    remark.setPublished(true);
//    remark.setAuthor(poolManagementUser.asReference());
//
//    requestor.createNode(remark);
//
//    System.out.println(remark);

    RoundRequestor  roundRequestor = new RoundRequestor(drupalSessionManager);
    Round           round          = roundRequestor.requestNodeByNid(26);
//
//    System.out.println(round);
//
//    round.setRoundStatus(Round.Status.OPEN);
//    round.getRoundDuration().setEndDate(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).parse("10/5/2013 6:35 AM"));
//
//    roundRequestor.updateNode(round);

    SolvedBlockRequestor requestor2 = new SolvedBlockRequestor(drupalSessionManager);
    SolvedBlock          block      = requestor2.requestNodeByNid(27);
//
//    System.out.println(block);

//    block.setCreationTime(new Date());
//    block.setReward(BigDecimal.valueOf(25));
//    block.setSolvingMember(new User.Reference(1));
//
//    requestor2.updateNode(block);
//
//    System.out.println(block);

    WorkShareRequestor  newRequestor = new WorkShareRequestor(drupalSessionManager);
    WorkShare           newShare = new WorkShare();

    newShare.setJobHash("ABC123");
    newShare.setBlock(block.asReference());
    newShare.setRound(round.asReference());
    newShare.setSubmitter(poolManagementUser.asReference());
    newShare.setDateSubmitted(new Date(2013, 10, 1, 4, 55, 55));
    newShare.setClientSoftwareVersion("cgminer 1.0");
    newShare.setPoolHost("sockthing/northeast");
    newShare.setVerifiedByPool(true);
    newShare.setVerifiedByNetwork(false);
    newShare.setStatus("stale");

    newRequestor.createNode(newShare);
  }
}