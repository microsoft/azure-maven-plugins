Param(
    [string] $link,
    [string[]] $logs
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

Function GetEmailContent($link, $logs) {
    $logFile = Get-Content $logs;
    $errorLog = $logFile -match "\[error\]";
    $errorContent = [string]::join("<br/>", $errorLog)
    $pageBody="
<html>
    <span>Please check the CI log in the attachment</span>
    <span>If you want to get more information, please refer <a href='$link'>Appveyor Job Log</a></span>
    <br/>
    <br/>
    {$errorContent}
    <br/>
    <br/>
    <span>This report is generated automatically. For any suggestions or issues, please contact hanli@microsoft.com.</span>
</html>
"
    return $pageBody;
}

#back to base folder
$base = $Env:APPVEYOR_BUILD_FOLDER;
cd $base;
#send email
$title="Azure Function Maven Plugin CI Issue";
$mailto=New-Object System.Collections.Generic.List[System.String];
$mailto.Add("andxu@microsoft.com");
$mailto.Add("Sheng.Chen@microsoft.com");
$mailto.Add("Rome.Li@microsoft.com");
$mailcc=New-Object System.Collections.Generic.List[System.String];
$mailcc.Add("hanli@microsoft.com");
$sender = 'insvsc@microsoft.com';

$pageBody=GetEmailContent $link $logs
$senderPassword=$env:EMAIL_PASSWORD
$attachments = New-Object System.Collections.Generic.List[System.String];
foreach($logFile in $logs) {
    $attachments.Add((Get-Item $logFile).FullName); 
}
[WindowsAPILib.OutlookDotComMail]::SendMail($mailto, $mailcc, $title, $sender, $senderPassword, $attachments, $pageBody);