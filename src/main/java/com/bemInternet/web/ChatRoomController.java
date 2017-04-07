package com.bemInternet.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bemInternet.Service.ChatService;
import com.bemInternet.Service.UserService;
import com.bemInternet.domain.Chat;
import com.bemInternet.domain.User;
import com.bemInternet.form.GetChatForm;

@Controller
public class ChatRoomController extends BaseController{
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ChatService chatService;
	
	@Resource
    private SimpMessagingTemplate simpMessagingTemplate; 
	
    @GetMapping("/chatRoom")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
	public String ChatIndex(Model model){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		user = userService.findUserByStudentld(auth.getName());
		List<User> Friends = chatService.QueryFriendsList(user);
		model.addAttribute("users", user);
		model.addAttribute("posts", Friends);
    	return "chatRoom";
    }
    
	@PostMapping("/initialization")
    public @ResponseBody Map<String, Object> get_user() throws Exception{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("thisUser", user.getStudentld());
		List<String> unreadUser = chatService.QueryMessageState(user.getStudentld());
		List<String> unreadUserNull = chatService.QueryMessageStateNull(user.getStudentld());
		map.put("unreadUser", unreadUser);
		map.put("unreadUserNull", unreadUserNull);
		return map;
    }
	
    @MessageMapping("/" + "sendChat")
    public void greeting(@Valid GetChatForm message) throws Exception{
    	Thread.sleep(1000); // simulated delay
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Inname", message.getInputname());
        map.put("Ouname", message.getOutputname());
        map.put("message", message.getMessage());
        this.simpMessagingTemplate.convertAndSend("/topic/" + message.getOutputname(), map);
    }
//    
    //消息保存数据库
    @PostMapping("/chat_check")
	public @ResponseBody Map<String, Object> chat_check(@Valid GetChatForm getchat){
		Map<String,Object> map = new HashMap<String,Object>();
		Date date=new Date(); 
		SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		Chat message = new Chat();
		message.setInputname(getchat.getInputname());
		message.setOutputname(getchat.getOutputname());
		message.setMessage(getchat.getMessage());
		message.setSendtime(df.format(date));
		message.setState(getchat.getState());
		chatService.SaveChat(message);
		map.put("msg", "success");
		map.put("inputname", getchat.getInputname());
		map.put("data", getchat.getMessage());
		return map;
	}
//    
    //保存消息状态
    @PostMapping("/save_state")
	public @ResponseBody Map<String, Object> chat_state(@Valid GetChatForm getchat){
    	Map<String,Object> map = new HashMap<String,Object>();
    	List<Chat> chat = chatService.SaveMessageState(getchat.getInputname(), getchat.getOutputname());
		if(getchat.getState().equals("1")){
			for(Chat item : chat){
				item.setState("1");
				chatService.SaveChat(item);
			}
		}else if(getchat.getState().equals("-1")){
			for(int i = 0; i < chat.size(); i++){
				if(i == chat.size() - 1){
					chat.get(i).setState("-1");
					chatService.SaveChat(chat.get(i));
				}
			}
		}
		map.put("msg", "success");
		return map;
    }
//    
//    //测试添加好友方法
//    @RequestMapping(value = "/test")
//    public @ResponseBody Map<String, Object> test(){
//    	Map<String,Object> map = new HashMap<String,Object>();
//		User user = userService.get("201300");
//		List<User> friends = user.getFriends();
//		User addFriend = userService.get("123456");
//		friends.add(addFriend);
//		user.setFriends(friends);
//		userService.register(user);
//		return map;
//    }
//    
    //当前好友消息记录
    @PostMapping("/get_message")
    public @ResponseBody Map<String, Object> get_message(Model model, @Valid GetChatForm getchat){
    	List<String> message = new ArrayList<String>();
    	List<String> thisUser = new ArrayList<String>();
    	List<String> thisTime = new ArrayList<String>();
    	Map<String,Object> map = new HashMap<String,Object>();
    	List<Chat> chat = chatService.QueryMessageList(getchat.getInputname(), getchat.getOutputname());
    	User outUser = userService.get(getchat.getOutputname());
    	User inUser = userService.get(getchat.getInputname());
    	for(Chat item : chat){
    		message.add(item.getMessage());
    		thisUser.add(item.getInputname());
    		thisTime.add(item.getSendtime());
    	}
    	map.put("message", message);
    	map.put("thisUser", thisUser);
    	map.put("thisTime", thisTime);
    	map.put("outputUser", getchat.getOutputname());
    	map.put("outputImg", outUser.getHeadimgfile());
    	map.put("inputImg", inUser.getHeadimgfile());
    	return map;
    }
}