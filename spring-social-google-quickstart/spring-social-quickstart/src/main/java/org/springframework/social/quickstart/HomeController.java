/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.quickstart;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.social.ExpiredAuthorizationException;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.gdata.contact.Contact;
import org.springframework.social.google.api.gdata.contact.ContactGroup;
import org.springframework.social.google.api.gdata.contact.Email;
import org.springframework.social.google.api.gdata.contact.Phone;
import org.springframework.social.google.api.gdata.query.GDataPage;
import org.springframework.social.google.api.legacyprofile.LegacyGoogleProfile;
import org.springframework.social.google.api.plus.activity.ActivitiesPage;
import org.springframework.social.google.api.plus.activity.Activity;
import org.springframework.social.google.api.plus.comment.Comment;
import org.springframework.social.google.api.plus.comment.CommentsPage;
import org.springframework.social.google.api.plus.person.PeoplePage;
import org.springframework.social.google.api.plus.person.Person;
import org.springframework.social.google.api.tasks.Task;
import org.springframework.social.google.api.tasks.TaskList;
import org.springframework.social.google.api.tasks.TaskListsPage;
import org.springframework.social.google.api.tasks.TasksPage;
import org.springframework.social.quickstart.contact.ContactForm;
import org.springframework.social.quickstart.contact.ContactGroupForm;
import org.springframework.social.quickstart.contact.ContactSearchForm;
import org.springframework.social.quickstart.contact.EmailForm;
import org.springframework.social.quickstart.contact.PhoneForm;
import org.springframework.social.quickstart.tasks.TaskForm;
import org.springframework.social.quickstart.tasks.TaskListForm;
import org.springframework.social.quickstart.tasks.TaskSearchForm;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

	private final Google google;
	
	@Autowired
	public HomeController(Google google) {
		this.google = google;
	}
	
	@ExceptionHandler(ExpiredAuthorizationException.class)
	public String handleExpiredToken() {
		return "redirect:/signout";
	}

	@RequestMapping(value="/", method=GET)
	public ModelAndView home() {
		
		LegacyGoogleProfile profile = google.userOperations().getUserProfile();
		
		return new ModelAndView("profile", "profile", profile);
	}
	
	@RequestMapping(value="contacts", method=GET)
	public ModelAndView contacts(ContactSearchForm command) {
		
		List<ContactGroup> groups = google.contactOperations().getContactGroupList();
		
		GDataPage<Contact> contacts = google.contactOperations().contactQuery()
			.searchFor(command.getText())
			.startingFromIndex(command.getStartIndex())
			.maxResultsNumber(command.getMaxResults())
			.updatedFrom(command.getUpdatedMin())
			.updatedUntil(command.getUpdatedMax())
			.onGroup(hasText(command.getGroupId()) ?new ContactGroup(command.getGroupId(), null, null, null) : null)
			.getPage();
		
		return new ModelAndView("contacts")
			.addObject("groups", groups)
			.addObject("contacts", contacts)
			.addObject("command", command);
	}
	
	@RequestMapping(value="groups", method=GET)
	public ModelAndView groups(SearchForm command) {
		
		GDataPage<ContactGroup> groups = google.contactOperations().contactGroupQuery()
			.startingFromIndex(command.getStartIndex())
			.maxResultsNumber(command.getMaxResults())
			.updatedFrom(command.getUpdatedMin())
			.updatedUntil(command.getUpdatedMax())
			.getPage();
		
		return new ModelAndView("groups")
			.addObject("groups", groups)
			.addObject("command", command);
	}
	
	@RequestMapping(value="group", method=GET)
	public ModelAndView addContactGroup() {
		return new ModelAndView("group", "command", new ContactGroupForm());
	}
	
	@RequestMapping(value="group", method=GET, params="url")
	public ModelAndView editContactGroup(@RequestParam(required=false) String url) {
		ContactGroup group = google.contactOperations().getContactGroup(url);
		ContactGroupForm command = new ContactGroupForm(group.getId(), group.getName(), group.getSelf());
		return new ModelAndView("group", "command", command);
	}
		
	@RequestMapping(value="group", method=POST)
	public ModelAndView saveContactGroup(@Valid ContactGroupForm command, BindingResult result) {
		
		if(result.hasErrors()) {
			return new ModelAndView("group", "command", command);
		}
		
		ContactGroup group = new ContactGroup(command.getId(), command.getName(), command.getUrl(), null);
		google.contactOperations().saveContactGroup(group);

		return new ModelAndView("redirect:/groups");
	}
	
	@RequestMapping(value="group", method=POST, params="delete")
	public String deleteContactGroup(@RequestParam String url) {
		google.contactOperations().deleteContactGroup(url);
		return "redirect:/groups";
	}
	
	@RequestMapping(value="contact", method=POST, params="delete")
	public String deleteContact(String url) {
		google.contactOperations().deleteContact(url);
		return "redirect:/contacts";
	}
	
	@RequestMapping(value="contact", method=GET)
	public ModelAndView addContact() {
		
		List<ContactGroup> allGroups = google.contactOperations().getContactGroupList();
		
		return new ModelAndView("contact", "command", new ContactForm())
			.addObject("allGroups", allGroups);
	}
	
	@RequestMapping(value="contact", method=GET, params="url")
	public ModelAndView editContact(@RequestParam String url) {
		
		Contact contact = google.contactOperations().getContact(url);
		ContactForm command = new ContactForm(
			contact.getId(), contact.getSelf(), contact.getNamePrefix(),
			contact.getFirstName(), contact.getMiddleName(), contact.getLastName(), 
			contact.getNameSuffix(), contact.getPictureUrl(), contact.getGroupIds());
		
		for(Email email : contact.getEmails()) {
			command.getEmails().add(new EmailForm(email.getRel(), email.getLabel(), email.getAddress(), email.isPrimary()));
		}
		
		for(Phone phone : contact.getPhones()) {
			command.getPhones().add(new PhoneForm(phone.getRel(), phone.getLabel(), phone.getNumber(), phone.isPrimary()));
		}
		
		List<ContactGroup> allGroups = google.contactOperations().getContactGroupList();
		
		return new ModelAndView("contact", "command", command)
			.addObject("allGroups", allGroups);
	}
	
	@RequestMapping(value="contact", method=POST)
	public ModelAndView saveContact(@Valid ContactForm command, BindingResult result) {
		
		if(result.hasErrors()) {
			return new ModelAndView("contact", "command", command);
		}
		
		List<Email> emails = new ArrayList<Email>();
		for(EmailForm e : command.getEmails()) {
			if(hasText(e.getAddress()) && hasText(e.getRel()) || hasText(e.getLabel())) {
				emails.add(new Email(e.getRel(), e.getLabel(), e.getAddress(), e.isPrimary()));
			}
		}
		
		List<Phone> phones = new ArrayList<Phone>();
		for(PhoneForm p : command.getPhones()) {
			if(hasText(p.getNumber()) && hasText(p.getRel()) || hasText(p.getLabel())) {
				phones.add(new Phone(p.getRel(), p.getLabel(), p.getNumber(), p.isPrimary()));
			}
		}
		
		Contact contact = new Contact(command.getId(), command.getUrl(), null, command.getNamePrefix(), 
				command.getFirstName(), command.getMiddleName(), command.getLastName(), 
				command.getNameSuffix(), command.getPictureUrl(), command.getGroupIds(), emails, phones);
		google.contactOperations().saveContact(contact);
		return new ModelAndView("redirect:/contacts");
	}

	@RequestMapping(value="contactpicture", method=GET)
	public ResponseEntity<byte[]> getProfilePicture(@RequestParam String url) {
		byte[] body = google.contactOperations().getProfilePicture(url);
		if(body != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(IMAGE_JPEG);
			headers.setCacheControl("no-cache");
			return new ResponseEntity<byte[]>(body, headers, OK);
		}
		return new ResponseEntity<byte[]>(NOT_FOUND);
	}
	
	@RequestMapping(value="contactpicture", method=POST)
	public String uploadProfilePicture(
			@RequestHeader String referer, @RequestParam String pictureUrl, 
			@RequestParam MultipartFile file) throws IOException {
		google.contactOperations().uploadProfilePicture(pictureUrl, file.getBytes());
		return "redirect:" + referer;
	}
	
	@RequestMapping(value="person", method=GET)
	public ModelAndView person(@RequestParam(required=false) String id) {
		if(hasText(id)) {
			Person person = google.personOperations().getPerson(id);
			return new ModelAndView("person")
				.addObject("command", new SearchForm())
				.addObject("person", person);
		}
		return new ModelAndView("redirect:/people");
	}
	
	@RequestMapping(value="people", method=GET, params={"!plusoners","!resharers"})
	public ModelAndView people(String text, String pageToken) {
		
		PeoplePage people;
		if(hasText(text)) {
			people = google.personOperations().personQuery()
				.searchFor(text)
				.fromPage(pageToken)
				.getPage();
		} else {
			people = new PeoplePage();
		}
		
		return new ModelAndView("people", "people", people);
	}
	
	@RequestMapping(value="people", method=GET, params="plusoners")
	public ModelAndView plusOners(String plusoners, String pageToken) {
		
		PeoplePage people = google.personOperations().getActivityPlusOners(plusoners, pageToken);
		return new ModelAndView("people", "people", people);
	}
	
	@RequestMapping(value="people", method=GET, params="resharers")
	public ModelAndView resharers(String resharers, String pageToken) {
		
		PeoplePage people = google.personOperations().getActivityPlusOners(resharers, pageToken);
		return new ModelAndView("people", "people", people);
	}
	
	@RequestMapping(value="activity", method=GET)
	public ModelAndView activity(String id) {
		
		Activity activity = google.activityOperations().getActivity(id);
		return new ModelAndView("activity", "activity", activity);
	}
	
	@RequestMapping(value="activities", method=GET, params="!text")
	public ModelAndView listActivities(@RequestParam(defaultValue="me") String person, String pageToken) {
		
		ActivitiesPage activities = google.activityOperations().getActivitiesPage(person, pageToken);
		
		return new ModelAndView("activities", "activities", activities);
	}
	
	@RequestMapping(value="activities", method=GET, params="text")
	public ModelAndView searchActivities(String text, String pageToken) {
		
		ActivitiesPage activities = google.activityOperations().activityQuery()
			.searchFor(text)
			.fromPage(pageToken)
			.getPage();
		
		return new ModelAndView("activities", "activities", activities);
	}
	
	@RequestMapping(value="comments", method=GET)
	public ModelAndView comments(String activity, String pageToken) {
		
		CommentsPage comments = google.commentOperations().getComments(activity, pageToken);
		return new ModelAndView("comments", "comments", comments);
	}
	
	@RequestMapping(value="comment", method=GET)
	public ModelAndView comment(String id) {
		
		Comment comment = google.commentOperations().getComment(id);
		return new ModelAndView("comment", "comment", comment);
	}
	
	@RequestMapping(value="tasklists", method=GET)
	public ModelAndView taskLists(String pageToken) {
		
		TaskListsPage taskLists = google.taskOperations().taskListQuery().fromPage(pageToken).getPage();
		
		return new ModelAndView("tasklists", "taskLists", taskLists);
	}
	
	@RequestMapping(value="tasklist", method=GET)
	public ModelAndView tasklist() {
		return new ModelAndView("tasklist", "command", new TaskListForm());
	}
	
	@RequestMapping(value="tasklist", method=GET, params="id")
	public ModelAndView taskList(String id) {

		TaskList taskList = google.taskOperations().getTaskList(id);
		TaskListForm command = new TaskListForm(taskList.getId(), taskList.getTitle());
		return new ModelAndView("tasklist", "command", command);
	}
	
	@RequestMapping(value="tasklist", method=POST)
	public ModelAndView saveTaskList(@Valid TaskListForm command, BindingResult result) {
		
		if(result.hasErrors()) {
			return new ModelAndView("tasklist", "command", command);
		}
		
		TaskList taskList = new TaskList(command.getId(), command.getTitle());
		google.taskOperations().saveTaskList(taskList);
		return new ModelAndView("redirect:/tasklists");
	}
	
	@RequestMapping(value="tasklist", method=POST, params="delete")
	public String deleteTaskList(TaskListForm command) {
		
		TaskList taskList = new TaskList(command.getId(), command.getTitle());
		google.taskOperations().deleteTaskList(taskList);
		return "redirect:/tasklists";
	}
	
	@RequestMapping(value="tasks", method=GET)
	public ModelAndView tasks(TaskSearchForm command) {
		
		TasksPage tasks = google.taskOperations().taskQuery()
				.fromTaskList(command.getList())
				.fromPage(command.getPageToken())
				.completedFrom(command.getCompletedMin())
				.completedUntil(command.getCompletedMax())
				.dueFrom(command.getDueMin())
				.dueUntil(command.getDueMax())
				.updatedFrom(command.getUpdatedMin())
				.includeCompleted(command.isIncludeCompleted())
				.includeDeleted(command.isIncludeDeleted())
				.includeHidden(command.isIncludeHidden())
				.getPage();
		
		return new ModelAndView("tasks")
			.addObject("command", command)
			.addObject("tasks", tasks);
	}
	
	@RequestMapping(value="task", method=GET)
	public ModelAndView task() {
		
		return new ModelAndView("task", "command", new TaskForm());
	}
	
	@RequestMapping(value="task", method=GET, params="id")
	public ModelAndView task(String list, String id) {
		
		if(!hasText(list)) {
			list = "@default";
		}
		
		Task task = google.taskOperations().getTask(list, id);
		TaskForm command = new TaskForm(task.getId(), task.getTitle(), task.getDue(), task.getNotes(), task.getCompleted());
		return new ModelAndView("task", "command", command);
	}
	
	@RequestMapping(value="task", method=POST)
	public ModelAndView saveTask(TaskForm command, BindingResult result) {
		
		if(result.hasErrors()) {
			return new ModelAndView("task", "command", command);
		}

		Task task = new Task(command.getId(), command.getTitle(), command.getNotes(), command.getDue(), command.getCompleted());
		google.taskOperations().saveTask(command.getList(), task);
		
		return new ModelAndView("redirect:/tasks", "list", command.getList());
	}
	
	@RequestMapping(value="task", method=POST, params="parent")
	public ModelAndView createTask(String parent, String previous, TaskForm command, BindingResult result) {
		
		if(result.hasErrors()) {
			return new ModelAndView("task", "command", command);
		}

		Task task = new Task(command.getId(), command.getTitle(), command.getNotes(), command.getDue(), command.getCompleted());
		google.taskOperations().createTaskAt(command.getList(), parent, previous, task);
		
		return new ModelAndView("redirect:/tasks", "list", command.getList());
	}
	
	@RequestMapping(value="movetask", method=POST)
	public ModelAndView moveTask(String list, String move, String parent, String previous) {
		
		google.taskOperations().moveTask(list, new Task(move), parent, previous);
		return new ModelAndView("redirect:/tasks", "list", list);
	}
	
	@RequestMapping(value="task", method=POST, params="delete")
	public ModelAndView deleteTask(@Valid TaskForm command) {
		
		google.taskOperations().deleteTask(command.getList(), new Task(command.getId()));
		return new ModelAndView("redirect:/tasks", "list", command.getList());
	}
	
	@RequestMapping(value="cleartasks", method=POST)
	public ModelAndView clearTasks(String list) {
		
		if(!hasText(list)) {
			list = "@default";
		}
		google.taskOperations().clearCompletedTasks(new TaskList(list, null));
		return new ModelAndView("redirect:/tasks", "list", list);
	}
}