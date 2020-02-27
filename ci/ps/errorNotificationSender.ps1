Param(
    [string] $project
)
$MailSenderType = @' 
namespace WindowsAPILib
{
    using System;
    using System.Collections.Generic;
    using System.Net;
    using System.Net.Mail;

    public static class OutlookDotComMail
    {
        public static void SendMail(List<string> mailtoUsers,List<string> mailccUsers , string subject, string sender, string senderPassword, List<string> attachments, string pageBody)
        {
            SmtpClient client = new SmtpClient("smtp.office365.com");
            client.Port = 587;
            client.DeliveryMethod = SmtpDeliveryMethod.Network;
            client.UseDefaultCredentials = false;
            NetworkCredential credentials = new NetworkCredential(sender, senderPassword);
            client.EnableSsl = true;
            client.Credentials = credentials;

            try
            {
                var mail = new MailMessage();
                mail.From = new MailAddress(sender.Trim());
                foreach (var mailtoUser in mailtoUsers)
                {
                   var userAddress = new MailAddress(mailtoUser.Trim());
                   mail.To.Add(userAddress);
                }
                foreach (var mailccUser in mailccUsers)
                {
                   var userAddress = new MailAddress(mailccUser.Trim());
                   mail.CC.Add(userAddress);
                }
                mail.Subject = subject;
                mail.IsBodyHtml = true;
                mail.Body = pageBody;
                foreach(var attachment in attachments)
                {
                   Attachment data = new Attachment(attachment);
                   mail.Attachments.Add(data);
                }
                client.Send(mail);
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                throw ex;
            }
        }
    }
}
'@
Add-Type -TypeDefinition $MailSenderType -Language CSharp;

Function CreateEventObject($reportXML) {
    $XMLObject = ([xml](Get-Content $reportXML)).'build-job'
    $result = [PSCustomObject]@{
        Project = $XMLObject.project.SubString(0, $XMLObject.project.Length - 8) # Remove  /pom.xml from project name
        Result  = IF ($XMLObject.result -eq "success") {"success"} Else {"fail"}
        Time    = $XMLObject.time
        Message = $XMLObject.failureMessage
    }
    return $result;
}

Function GetEmailContent($project, $resultList) {
    $link = $Env:FUNCTION_URL
    $resultTable = [string]::join(" ", ($resultList | ForEach-Object { GetReportRow $_ }))  
    $pageBody = "
    <html>
    <head>
      <style>
        table {
          border-collapse: collapse;
        }
    
        th,
        td {
          text-align: left;
          padding: 8px;
        }
        
        tr:nth-child(even) {background-color: #f2f2f2;}
    
        tr:hover {
          background-color: #f5f5f5;
        }
      </style>
    </head>
    <body>
      <p>Please check the CI log for azure-maven-plugin in the attachment</p>
      <p>If you want to get more information, please refer <a href='$link'>Appveyor Job Log</a></p>
      <br />
      <div>
        <table>
          <thead>
            <tr>
              <th>Project</th>
              <th>Result</th>
              <th>Time</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            $resultTable
          </tbody>
        </table>
      </div>
      <br />
      <span>This report is generated automatically. For any suggestions or issues, please contact
        hanli@microsoft.com.</span>
    </body>
    </html>
"
    return $pageBody;
}

Function GetReportRow($report) {
    $resultWithColor = IF ($report.result -eq "success") {"<font color='green'>success</font>"} Else {"<font color='red'>fail</font>"}
    $result = "<tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td></tr>" -f $report.Project, $resultWithColor, $report.Time, $report.Message
    return $result
}

#back to base folder
$base = $Env:APPVEYOR_BUILD_FOLDER;
cd $base;

#send email
$title = "Azure Maven Plugin CI Issue";
$mailto = New-Object System.Collections.Generic.List[System.String];
$mailto.Add("andxu@microsoft.com");
$mailto.Add("hanli@microsoft.com");
$mailcc = New-Object System.Collections.Generic.List[System.String];
$mailcc.Add("Sheng.Chen@microsoft.com");
$mailcc.Add("Rome.Li@microsoft.com");
$sender = "insvsc@microsoft.com";
$senderPassword = $env:EMAIL_PASSWORD

$reports = Get-ChildItem "$base\$project\target\invoker-reports\" -Filter "*.xml" | ForEach-Object { CreateEventObject "$base\$project\target\invoker-reports\$_" };
$pageBody = GetEmailContent $project $reports
$attachments = New-Object System.Collections.Generic.List[System.String];
foreach ($report in $reports) {
    IF ($report.Result -eq "fail") {
        Rename-Item ("{0}\{1}\target\it\{2}\build.log" -f $base, $project, $report.Project) ("{0}-build.log" -f $report.Project)
        $attachments.Add(("{0}\{1}\target\it\{2}\{2}-build.log" -f $base, $project, $report.Project )); 
    }
}

[WindowsAPILib.OutlookDotComMail]::SendMail($mailto, $mailcc, $title, $sender, $senderPassword, $attachments, $pageBody);